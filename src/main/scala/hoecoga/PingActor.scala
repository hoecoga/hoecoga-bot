package hoecoga

import akka.actor.{Actor, ActorLogging, Props}
import hoecoga.PingActor.{Ping, PingConfig, Pong}
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

  case class Ping(message: MessageEvent, args: Array[String])
  case object Pong

  def props(): Props = Props(new PingActor())
}
