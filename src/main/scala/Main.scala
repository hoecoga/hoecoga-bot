import java.util.UUID

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Guice}
import com.typesafe.config.ConfigFactory
import hoecoga.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga._
import hoecoga.scheduler.{JobData, SlackJob}
import hoecoga.slack.SlackWebApi
import hoecoga.websocket.ClientFactory

import scala.util.control.NonFatal

object Main {
  def main(args: Array[String]): Unit = {
    val config = new Config(ConfigFactory.load)
    val system = ActorSystem("slack-bot")
    val logger = system.log
    logger.info(s"running slack-bot")

    sys.addShutdownHook {
      logger.info("slack-bot shutdown hook")
      system.shutdown()
      try {
        system.awaitTermination(config.awaitTermination)
      } catch {
        case NonFatal(e) =>
          logger.error(s"system.awaitTermination(${config.awaitTermination})", e)
      }
    }

    val simpleMessageBus = new SimpleMessageEventBus
    val schedulerBus = new SchedulerEventBus

    val injector = Guice.createInjector(new AbstractModule {
      override def configure(): Unit = {
        bind(classOf[SlackJob]).toInstance(new SlackJob {
          override def tell(a: JobData): Unit = {
            simpleMessageBus.publish(SimpleMessageEvent(a.channel, a.message))
          }
        })
      }
    })

    val factory = new ClientFactory {}

    val api = new SlackWebApi(config)

    val schedulerSettings = SchedulerActor.SchedulerActorSettings(
      newJobId = {() => UUID.randomUUID().toString},
      injector = injector,
      timeZone = config.scheduler.timeZone,
      bus = schedulerBus)

    val slackSettings = SlackActor.SlackActorSettings(
      config.slack.ignoredChannels,
      config.slack.reconnectInterval,
      factory,
      api,
      simpleMessageBus,
      schedulerBus)

    system.actorOf(SchedulerActor.props(schedulerSettings))
    system.actorOf(SlackActor.props(slackSettings))
  }
}
