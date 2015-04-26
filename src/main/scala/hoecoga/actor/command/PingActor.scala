package hoecoga.actor.command

import akka.actor.{Actor, ActorLogging, Props}
import hoecoga.actor.command.PingActor.{Pong, Ping, PingConfig}
import hoecoga.slack.MessageEvent
import scopt.OptionParser

/**
 * A ping command actor.
 */
class PingActor extends Actor with ActorLogging with Cli[PingConfig] {
  override protected val parser = new OptionParser[PingConfig]("ping") with Parser {
    head("ping", "--", "sends back a pong message")

    opt[Unit]('h', "help").action((_, c) => c.copy(help = true)).text("prints this usage text")
  }

  override def receive: Receive = {
    case ping @ Ping(e, args) =>
      log.debug(s"""$ping""")
      parser.parse(args, PingConfig()) match {
        case Some(config) =>
          if (config.help) {
            sendUsage(sender())
          } else {
            sender() ! Pong
          }

        case None => sendError(sender())
      }
  }
}

object PingActor {
  case class PingConfig(help: Boolean = false)

  /**
   * An incoming ping command.
   * @param message original [[MessageEvent]].
   * @param args ping command arguments.
   */
  case class Ping(message: MessageEvent, args: Array[String])

  /**
   * The outgoing pong message.
   */
  case object Pong

  def props(): Props = Props(new PingActor())
}
