package hoecoga

import akka.actor.{Actor, ActorLogging, Props}
import hoecoga.CronActor._
import hoecoga.SchedulerActor.{CreateJob, DeleteJob, GetJobs}
import hoecoga.slack.MessageEvent
import scopt.OptionParser

/**
 * A cron command actor.
 */
class CronActor(settings: CronActorSettings) extends Actor with ActorLogging with Cli[CronConfig] {
  import settings._

  override protected val parser = new OptionParser[CronConfig]("cron") with Parser {
    head("cron", "--", "daemons to send scheduled simple messages")

    opt[Unit]('h', "help").action((_, c) => c.copy(help = true)).text("prints this usage text")

    cmd("list").action((_, c) => c.copy(command = Some(ListCommand))).text("prints cron jobs")

    cmd("create").action((_, c) => c.copy(command = Some(CreateCommand))).text("creates a new cron job").children {
      arg[String]("<cron expr>...").minOccurs(cronExpressionSize + 1).unbounded().
        action((s, c) => c.copy(createArgs = c.createArgs :+ s)).
        text("seconds, minutes, hours, day-of-month, month, day-of-week, and simple text, respectively (for more detail, refer to http://www.quartz-scheduler.org/generated/2.2.1/html/qs-all/#page/Quartz_Scheduler_Documentation_Set/co-trg_crontriggers.html)")
    }

    cmd("delete").action((_, c) => c.copy(command = Some(DeleteCommand))).text("deletes a cron job").children {
      arg[String]("<id>").minOccurs(1).maxOccurs(1).action((s, c) => c.copy(deleteArg = s)).text("job identifier")
    }
  }

  /**
   * @see [[https://api.slack.com/docs/formatting]]
   */
  private[this] def detect(s: String): String = {
    "<(.*?)>".r.replaceAllIn(s, {m =>
      val sub = m.group(0)
      if (sub.startsWith("<#C") || sub.startsWith("<@U") || sub.startsWith("<!")) {
        sub
      } else {
        val head = "(<)(.*)(|.*)?(>)".r
        sub match {
          case head(_, real, _, _) => real
        }
      }
    })
  }

  override def receive: Receive = {
    case cron @ Cron(e, args) =>
      log.debug(cron.toString)
      parser.parse(args, CronConfig()) match {
        case Some(config) =>
          if (config.help) {
            sendUsage(sender())
          } else {
            config.command match {
              case Some(command) =>
                command match {
                  case ListCommand =>
                    bus.publish(GetJobs(e.channel))

                  case CreateCommand =>
                    val (cron, text) = config.createArgs.splitAt(cronExpressionSize)
                    val job = CreateJob(
                      e.channel,
                      cron.mkString(" "),
                      detect(text.mkString(" ")))
                    bus.publish(job)

                  case DeleteCommand =>
                    bus.publish(DeleteJob(e.channel, config.deleteArg))
                }

              case None => sendError(sender())
            }
          }

        case None => sendError(sender())
      }
  }
}

object CronActor {
  private val cronExpressionSize = 6

  trait CronCommand

  private case object ListCommand extends CronCommand
  private case object CreateCommand extends CronCommand
  private case object DeleteCommand extends CronCommand

  case class CronConfig(help: Boolean = false, command: Option[CronCommand] = None, createArgs: List[String] = Nil, deleteArg: String = "")

  case class Cron(message: MessageEvent, args: Array[String])

  case class CronActorSettings(bus: SchedulerEventBus)

  def props(settings: CronActorSettings): Props = Props(new CronActor(settings))
}
