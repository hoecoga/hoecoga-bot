package hoecoga

import akka.actor.ActorRef
import hoecoga.Cli.{Error, Usage}
import scopt.OptionParser

/**
 * A command line interface like command parser.
 */
trait Cli[A] {
  private[this] var errors: List[String] = List.empty

  protected trait Parser extends OptionParser[A] {
    override def showUsageOnError: Boolean = false

    override def reportError(msg: String): Unit = {
      errors = msg :: errors
    }

    override def reportWarning(msg: String): Unit = {}

    override def showTryHelp: Unit = ()
  }

  protected val parser: Parser

  private[this] def usageLines(): Array[String] = parser.usage.split("\n")
  
  protected def sendUsage(ref: ActorRef): Unit = {
    ref ! Usage(text = usageLines().map(s => s">$s").mkString("\n"))
  }

  protected def sendError(ref: ActorRef): Unit = {
    ref ! Error(text = errors.reverse.mkString("\n"))
    errors = List.empty
  }
}

object Cli {
  case class Usage(text: String)
  case class Error(text: String)
}
