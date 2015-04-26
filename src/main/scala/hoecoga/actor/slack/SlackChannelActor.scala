package hoecoga.actor.slack

import akka.actor._
import hoecoga.actor.command.CronActor.{Cron, CronActorSettings}
import hoecoga.actor.command.PingActor.{Ping, Pong}
import hoecoga.actor.command.{Cli, CronActor, PingActor}
import hoecoga.actor.scheduler.SchedulerEventBus
import hoecoga.actor.slack.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.actor.slack.SlackChannelActor.ChannelMessageEvent
import hoecoga.slack.{MessageEvent, SlackChannel, SlackChannelName, SlackUser}

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
  /**
   * An incoming [[MessageEvent]] with [[SlackChannelName]] for each slack channel.
   * @param bot the slack bot user.
   */
  case class ChannelMessageEvent(bot: SlackUser, message: MessageEvent, name: SlackChannelName)

  case class SlackChannelActorSettings(
    channel: SlackChannel,
    simpleMessageBus: SimpleMessageEventBus,
    schedulerBus: SchedulerEventBus)

  def props(settings: SlackChannelActorSettings): Props = Props(new SlackChannelActor(settings))
}
