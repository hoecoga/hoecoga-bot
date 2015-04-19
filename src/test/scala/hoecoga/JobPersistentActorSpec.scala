package hoecoga

import java.nio.file.Path
import java.util.UUID

import akka.testkit.TestProbe
import hoecoga.core.ArbitraryHelper
import hoecoga.scheduler.JobPersistentActor.{Delete, Insert, Query, QueryResult}
import hoecoga.scheduler.{JobData, JobPersistentActor}
import hoecoga.slack.SlackChannel
import org.scalatest.FunSpec

class JobPersistentActorSpec extends FunSpec with ArbitraryHelper with JobPersistentActorSpecHelper {
  describe("JobPersistentActor") {
    it("commands") {
      val id = UUID.randomUUID().toString
      val dir = tempDirectory()
      val channel = sample[SlackChannel]
      val data1, data2, data3 = sample[JobData].copy(channel = channel)
      test(id, dir) { settings =>
        import settings._

        command(Insert(data1))

        command(Query(channel))
        probe.expectMsg(QueryResult(channel, List(data1)))

        command(Insert(data2))
        command(Insert(data3))
        command(Delete(channel, data2.id))

        command(Query(channel))
        probe.expectMsg(QueryResult(channel, List(data3, data1)))
      }

      test(id, dir) { settings =>
        import settings._

        command(Query(channel))
        probe.expectMsg(QueryResult(channel, List(data3, data1)))

        command(Insert(data2))

        command(Query(channel))
        probe.expectMsg(QueryResult(channel, List(data2, data3, data1)))

        val emptyChannel = sample[SlackChannel]
        command(Query(emptyChannel))
        probe.expectMsg(QueryResult(emptyChannel, Nil))

        command(Delete(emptyChannel, data2.id))
        command(Query(channel))
        probe.expectMsg(QueryResult(channel, List(data2, data3, data1)))
      }
    }
  }
}

trait JobPersistentActorSpecHelper extends PersistentActorHelper {
  case class JobPersistentActorSpecSettings(probe: TestProbe, command: Any => Unit)

  def test(id: String, temp: Path)(f: JobPersistentActorSpecSettings => Unit) = {
    val system = createActorSystem(temp)
    val settings = JobPersistentActor.JobPersistentActorSettings(id = id, {_ => }, {() => })
    val actor = system.actorOf(JobPersistentActor.props(settings))
    val probe = new TestProbe(system)

    def command(any: Any): Unit = probe.send(actor, any)

    try {
      f(JobPersistentActorSpecSettings(probe, command))
    } finally {
      system.shutdown()
    }
  }
}
