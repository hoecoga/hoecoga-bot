package hoecoga.actor.slack

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import hoecoga.actor.scheduler.SchedulerEventBus
import hoecoga.actor.slack.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.actor.slack.SlackChannelActor.ChannelMessageEvent
import hoecoga.core.ArbitraryHelper
import hoecoga.slack.{SlackChannelName, MessageEvent, SlackChannel, SlackUser}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike}

class SlackChannelActorSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender with FunSpecLike with BeforeAndAfterAll with SlackChannelActorHelper {

  def this() = this(ActorSystem("SlackChannelActorSpec"))

  override def afterAll(): Unit =  {
    TestKit.shutdownActorSystem(system)
  }

  describe("SlackChannelActor") {
    it("ping") {
      test { settings: SlackChannelActorSpecSettings =>
        import settings._

        def ping(args: String) = actor ! message(s"ping $args")

        ping("")
        expectEvent("pong")

        Seq("-h", "--help").foreach { args =>
          ping(args)
          expectUsage {
            s"""ping -- sends back a pong message
               |Usage: ping [options]
               |
               |  -h | --help
               |        prints this usage text""".stripMargin
          }
        }

        ping("x y")
        expectEvent("Unknown argument 'x'\nUnknown argument 'y'")
      }
    }

    it("cron") {
      test { settings: SlackChannelActorSpecSettings =>
        import settings._

        def cron(args: String) = actor ! message(s"cron $args")

        Seq("-h", "--help").foreach { args =>
          cron(args)
          expectUsage {
            s"""cron -- daemons to send scheduled simple messages
               |Usage: cron [list|create|delete] [options] <args>...
               |
               |  -h | --help
               |        prints this usage text
               |Command: list
               |prints cron jobs
               |Command: create <cron expr>...
               |creates a new cron job
               |  <cron expr>...
               |        seconds, minutes, hours, day-of-month, month, day-of-week, and simple text, respectively (for more detail, refer to http://www.quartz-scheduler.org/generated/2.2.1/html/qs-all/#page/Quartz_Scheduler_Documentation_Set/co-trg_crontriggers.html)
               |Command: delete <id>
               |deletes a cron job
               |  <id>
               |        job identifier""".stripMargin
          }
        }

        cron("x y")
        expectEvent("Unknown argument 'x'\nUnknown argument 'y'")
      }
    }
  }
}

trait SlackChannelActorHelper extends ArbitraryHelper {
  case class SlackChannelActorSpecSettings(
    actor: ActorRef,
    message: String => ChannelMessageEvent,
    expectEvent: String => SimpleMessageEvent,
    expectUsage: String => SimpleMessageEvent
  )

  def test(f: SlackChannelActorSpecSettings => Unit)(implicit system: ActorSystem): Unit = {
    val probe = TestProbe()
    val bot = sample[SlackUser]
    val channel = sample[SlackChannel]
    val name = sample[SlackChannelName]
    val simpleMessageBus = new SimpleMessageEventBus
    val schedulerBus = new SchedulerEventBus

    val actor = {
      val settings = SlackChannelActor.SlackChannelActorSettings(channel, simpleMessageBus, schedulerBus)
      system.actorOf(SlackChannelActor.props(settings))
    }

    simpleMessageBus.subscribe(probe.ref, classOf[SimpleMessageEvent])

    def message(text: String): ChannelMessageEvent =
      ChannelMessageEvent(bot, sample[MessageEvent].copy(channel = channel, text = s"<@${bot.id}>: $text"), name)

    def expect(text: String): SimpleMessageEvent = probe.expectMsg(SimpleMessageEvent(channel, text))

    def usage(text: String): SimpleMessageEvent = expect(text.split("\n").map(s => s">$s").mkString("\n"))

    try {
      f(SlackChannelActorSpecSettings(actor, message, expect, usage))
    } finally {
      actor ! PoisonPill
    }
  }
}
