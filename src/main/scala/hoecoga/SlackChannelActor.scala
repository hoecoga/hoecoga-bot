package hoecoga

import akka.actor._
import hoecoga.CronActor.{Cron, CronActorSettings}
import hoecoga.PingActor.{Ping, Pong}
import hoecoga.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.SlackChannelActor.ChannelMessageEvent
import hoecoga.slack.{MessageEvent, SlackChannel, SlackUser}
import org.scalacheck.{Arbitrary, Gen}

/**
 * A slack channel actor.
 */
class SlackChannelActor(settings: SlackChannelActor.SlackChannelActorSettings) extends Actor with ActorLogging {
  import settings._

  private[this] val pingActor = context.actorOf(PingActor.props())

  private[this] val cronActor = context.actorOf(CronActor.props(CronActorSettings(schedulerBus)))

  private[this] def send(s: String) = simpleMessageBus.publish(SimpleMessageEvent(channel, s))

  override def receive: Receive = {
    case ChannelMessageEvent(bot, message, name) =>
      log.info(s"#$name: $bot receives $message")
      val ping = s"(<@${bot.id}>:[ ]*ping[ ]*)(.*)".r
      val cron = s"(<@${bot.id}>:[ ]*cron[ ]*)(.*)".r
      message.text match {
        case ping(_, arg) => pingActor ! Ping(message, arg.split("[ \\t]").filterNot(_.isEmpty))
        case cron(_, arg) => cronActor ! Cron(message, arg.split("[ \\t]").filterNot(_.isEmpty))
        case _ =>
      }

    case Cli.Usage(text) => send(text)

    case Cli.Error(text) => send(text)

    case Pong => send("pong")
  }
}

object SlackChannelActor {
  case class ChannelName(name: String)

  implicit val arbitrary: Arbitrary[ChannelName] = Arbitrary(Gen.identifier.map(ChannelName))

  case class ChannelMessageEvent(bot: SlackUser, message: MessageEvent, name: ChannelName)

  case class SlackChannelActorSettings(
    channel: SlackChannel,
    simpleMessageBus: SimpleMessageEventBus,
    schedulerBus: SchedulerEventBus)

  def props(settings: SlackChannelActorSettings): Props =
    Props(new SlackChannelActor(settings))
}
