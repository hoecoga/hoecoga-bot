package hoecoga

import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import hoecoga.SchedulerActor.{CreatedJob, DeletedJob, Jobs, SchedulerFailure}
import hoecoga.SchedulerEventBus.OutgoingSchedulerEvent
import hoecoga.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.SlackActor._
import hoecoga.SlackChannelActor.{ChannelMessageEvent, ChannelName}
import hoecoga.SlackKeepAliveActor.{ExceedInterval, AskKeepAlive, Alive, SlackKeepAliveActorSettings}
import hoecoga.slack._
import hoecoga.websocket.{Client, ClientFactory}
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable
import scala.concurrent.duration._

/**
 * A slack websocket actor.
 */
class SlackActor(settings: SlackActor.SlackActorSettings) extends Actor with ActorLogging {
  import settings._
  import context.dispatcher

  private[this] val simpleMessageId = new AtomicLong(0)

  private[this] def nextMessageId() = simpleMessageId.incrementAndGet()

  private[this] var client: Client = null

  private[this] var bot: SlackUser = null

  private[this] var keepAlive: ActorRef = null

  private[this] val channels: mutable.Map[SlackChannel, ActorRef] = mutable.Map.empty

  private[this] val channelNames: mutable.Map[SlackChannel, String] = mutable.Map.empty

  private[this] def withChannelName(channel: SlackChannel)(f: ChannelName => Unit): Unit = {
    val name = channelNames.getOrElseUpdate(channel, api.info(channel))
    if (!ignoredChannels.contains(name)) f(ChannelName(name)) else log.debug(s"#$name ignored")
  }

  private[this] def event(e: JsValue): Unit = {
    (e \ "type").asOpt[String] match {
      case Some("message") =>
        self ! e.as[MessageEvent]

      case Some("pong") =>
        self ! e.as[PongEvent]

      case _ =>
    }
    if (keepAlive != null) keepAlive ! Alive
    log.debug(e.toString().take(1000))
  }

  private[this] def channelActor(channel: SlackChannel): ActorRef = {
    val settings = SlackChannelActor.SlackChannelActorSettings(channel, simpleMessageBus, schedulerBus)
    channels.getOrElseUpdate(
      channel,
      context.actorOf(SlackChannelActor.props(settings)))
  }

  override def preStart(): Unit = {
    super.preStart()
    self ! Connect
    simpleMessageBus.subscribe(self, classOf[SimpleMessageEvent])
    Seq(classOf[Jobs], classOf[CreatedJob], classOf[DeletedJob], classOf[SchedulerFailure]).foreach(schedulerBus.subscribe(self, _))
  }

  override def postStop(): Unit = {
    super.postStop()

    simpleMessageBus.unsubscribe(self)
    schedulerBus.unsubscribe(self)

    if (client != null) client.close()

    log.info("slack terminated")
  }

  override def receive: Receive = {
    case Connect =>
      val (uri, user) = api.start()

      def open(h: ServerHandshake): Unit = {
        log.info(s"open: uri=$uri")
      }

      def error(e: Exception): Unit = {
        log.error(e, s"error: uri=$uri")
      }

      def close(code: Int, reason: String, remote: Boolean): Unit = {
        log.info(s"close: uri=$uri, code=$code, reason=$reason, remote=$remote")
        self ! Reconnect
      }

      client = factory.wss(uri = uri, open = open, error = error, close = close, receive = event)
      bot = user
      keepAlive = context.actorOf(SlackKeepAliveActor.props(SlackKeepAliveActorSettings(self, keepAliveInterval)))

    case Reconnect =>
      client = null
      bot = null
      keepAlive ! PoisonPill
      keepAlive = null
      context.system.scheduler.scheduleOnce(reconnectInterval, self, Connect)

    case AskKeepAlive =>
      val id = nextMessageId()
      if (client != null) {
        log.debug(s"send(${Json.toJson(PingMessage(id)).toString()})")
        client.send(Json.toJson(PingMessage(id)).toString())
      }

    case ExceedInterval =>
      if (client != null) client.close()
      log.info("connection closed due to exceed keep alive interval")

    case e @ PongEvent(replyTo, _) =>
      if (keepAlive != null) keepAlive ! e

    case e @ MessageEvent(channel, _, _, _) =>
      log.debug(s"$e")
      withChannelName(channel) { name =>
        if (bot == e.user) {
          log.info(s"#$name: $e recursively")
        } else {
          channelActor(channel) ! ChannelMessageEvent(bot, e, name)
        }
      }

    case e: SimpleMessageEvent =>
      withChannelName(e.channel) { name =>
        log.info(s"#$name: $bot sends $e")
        val message = SimpleMessage(channel = e.channel, text = e.text, id = nextMessageId())
        if (client != null) client.send(Json.toJson(message).toString())
        else log.info(s"connection closed: #$name: $e")
      }

    case e: OutgoingSchedulerEvent =>
      simpleMessageBus.publish(e.simpleMessageEvent)
  }
}

object SlackActor {
  private case object Connect
  private case object Reconnect

  /**
   * @param ignoredChannels refer to [[Config.slack.ignoredChannels]].
   * @param reconnectInterval refer to [[Config.slack.reconnectInterval]].
   */
  case class SlackActorSettings(
    keepAliveInterval: FiniteDuration,
    ignoredChannels: List[String],
    reconnectInterval: FiniteDuration,
    factory: ClientFactory,
    api: SlackWebApi,
    simpleMessageBus: SimpleMessageEventBus,
    schedulerBus: SchedulerEventBus)

  def props(settings: SlackActorSettings): Props =
    Props(new SlackActor(settings))
}
