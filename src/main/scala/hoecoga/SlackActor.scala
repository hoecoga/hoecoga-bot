package hoecoga

import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import hoecoga.SchedulerActor.{CreatedJob, DeletedJob, Jobs, SchedulerFailure}
import hoecoga.SchedulerEventBus.OutgoingSchedulerEvent
import hoecoga.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.SlackActor.{Connect, Reconnect, SimpleMessage}
import hoecoga.SlackChannelActor.{ChannelMessageEvent, ChannelName}
import hoecoga.core.JsonUtil
import hoecoga.slack.{MessageEvent, SlackChannel, SlackUser, SlackWebApi}
import hoecoga.websocket.{Client, ClientFactory}
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.{Format, JsValue, Json}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
 * A slack websocket actor.
 */
class SlackActor(settings: SlackActor.SlackActorSettings) extends Actor with ActorLogging {
  import settings._

  private[this] val simpleMessageId = new AtomicLong(0)

  private[this] var client: Client = null

  private[this] var bot: SlackUser = null

  private[this] val channels: mutable.Map[SlackChannel, ActorRef] = mutable.Map.empty

  private[this] val channelNames: mutable.Map[SlackChannel, String] = mutable.Map.empty

  private[this] def withChannelName(channel: SlackChannel)(f: ChannelName => Unit): Unit = {
    val name = channelNames.getOrElseUpdate(channel, api.info(channel))
    if (!ignoredChannels.contains(name)) f(ChannelName(name)) else log.debug(s"#$name ignored")
  }

  private[this] def event(e: JsValue): Unit = {
    if ((e \ "type").asOpt[String].contains("message")) {
      self ! e.as[MessageEvent]
    }
    log.debug(e.toString())
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
        log.error(s"error: uri=$uri", e)
      }

      def close(code: Int, reason: String, remote: Boolean): Unit = {
        log.info(s"close: uri=$uri, code=$code, reason=$reason, remote=$remote")
        self ! Reconnect
      }

      client = factory.wss(uri = uri, open = open, error = error, close = close, receive = event)
      bot = user

    case Reconnect =>
      client = null
      bot = null
      Thread.sleep(reconnectInterval.toMillis)
      self ! Connect

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
        val message = SimpleMessage(channel = e.channel, text = e.text, id = simpleMessageId.incrementAndGet())
        if (client != null) client.send(Json.toJson(message).toString())
        else log.error(s"connection closed: #$name: $e")
      }

    case e: OutgoingSchedulerEvent =>
      simpleMessageBus.publish(e.simpleMessageEvent)
  }
}

object SlackActor {
  private case object Connect
  private case object Reconnect

  /**
   * @see [[https://api.slack.com/rtm]]
   */
  case class SimpleMessage(id: Long, channel: SlackChannel, text: String, tpe: String = "message") {
    require(tpe == "message")
  }

  implicit val format: Format[SimpleMessage] =
    JsonUtil.format("id", "channel", "text", "type", SimpleMessage.apply, SimpleMessage.unapply)

  /**
   * @param ignoredChannels refer to [[Config.slack.ignoredChannels]].
   * @param reconnectInterval refer to [[Config.slack.reconnectInterval]].
   */
  case class SlackActorSettings(
    ignoredChannels: List[String],
    reconnectInterval: FiniteDuration,
    factory: ClientFactory,
    api: SlackWebApi,
    simpleMessageBus: SimpleMessageEventBus,
    schedulerBus: SchedulerEventBus)

  def props(settings: SlackActorSettings): Props =
    Props(new SlackActor(settings))
}
