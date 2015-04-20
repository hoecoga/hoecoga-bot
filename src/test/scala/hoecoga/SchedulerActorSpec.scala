package hoecoga

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference
import java.util.{TimeZone, UUID}

import akka.testkit.TestProbe
import com.google.inject.{AbstractModule, Guice}
import hoecoga.SchedulerActor._
import hoecoga.core.ArbitraryHelper
import hoecoga.scheduler.{JobData, SlackJob}
import hoecoga.slack.SlackChannel
import org.scalatest.FunSpec

class SchedulerActorSpec extends FunSpec with ArbitraryHelper with SchedulerActorSpecHelper {
  describe("SchedulerActor") {
    it("cron triggers") {
      test { settings =>
        import settings._

        val channel1, channel2, channel3 = sample[SlackChannel]
        val message1, message2, message3 = sample[String]
        val now = LocalDateTime.now()
        val now1 = now.plusSeconds(3)
        val now2 = now.plusSeconds(4)
        val now3 = now.plusSeconds(5)
        val cron1 = s"${now1.getSecond} * * * * ?"
        val cron2 = s"${now2.getSecond} * * * * ?"
        val cron3 = s"${now3.getSecond} * * * * ?"
        val values =  Seq((channel1, cron1, message1), (channel2, cron2, message2), (channel3, cron3, message3))

        values.foreach {
          case (channel, cron_, message) =>
            cron(channel, cron_, message)
        }
        values.foreach {
          case (channel, cron_, message) =>
            schedulerObserver.expectMsg(CreatedJob(channel, JobData(channel, jobId.get(), cron_, message)))
        }

        delete(channel2, jobId.get())
        schedulerObserver.expectMsg(DeletedJob(channel2, jobId.get(), true))

        (values.head :: values.last :: Nil).foreach {
          case (channel, cron_, message) =>
            messageObserver.expectMsg(JobData(channel, jobId.get(), cron_, message))
        }
      }
    }
  }
}

trait SchedulerActorSpecHelper {
  case class SchedulerActorSpecSettings(
    schedulerObserver: TestProbe,
    messageObserver: TestProbe,
    jobId: AtomicReference[String],
    cron: (SlackChannel, String, String) => Unit,
    delete: (SlackChannel, String) => Unit
  )

  def test(f: SchedulerActorSpecSettings => Unit) = {
    val jobId = new AtomicReference[String](UUID.randomUUID().toString)
    val system = PersistentActorHelper.createActorSystem()
    val probe1, probe2 = new TestProbe(system)
    val injector = Guice.createInjector(new AbstractModule {
      override def configure(): Unit = {
        bind(classOf[SlackJob]).toInstance(new SlackJob {
          override def tell(a: JobData): Unit = probe2.ref ! a
        })
      }
    })
    val bus = new SchedulerEventBus
    val settings = SchedulerActorSettings({() => jobId.get()}, injector, TimeZone.getDefault, bus)
    val actor = system.actorOf(SchedulerActor.props(settings))

    Seq(classOf[CreatedJob], classOf[DeletedJob]).foreach(bus.subscribe(probe1.ref, _))

    def cron(channel: SlackChannel, cron: String, message: String): Unit = actor ! CreateJob(channel, cron, message)

    def del(channel: SlackChannel, id: String): Unit = actor ! DeleteJob(channel, id)

    try {
      f(SchedulerActorSpecSettings(probe1, probe2, jobId, cron, del))
    } finally {
      system.shutdown()
      system.awaitTermination()
    }
  }
}
