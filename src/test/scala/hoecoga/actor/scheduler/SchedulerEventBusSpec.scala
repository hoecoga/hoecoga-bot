package hoecoga.actor.scheduler

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import hoecoga.actor.scheduler.SchedulerActor.GetJobs
import hoecoga.actor.scheduler.SchedulerEventBus.IncomingSchedulerEvent
import hoecoga.core.ArbitraryHelper
import hoecoga.slack.SlackChannel
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterAll, FunSpecLike}

class SchedulerEventBusSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender with FunSpecLike with BeforeAndAfterAll with ArbitraryHelper {

  def this() = this(ActorSystem("SchedulerEventBusSpec"))

  override def afterAll(): Unit =  {
    TestKit.shutdownActorSystem(system)
  }

  describe("SchedulerEventBus") {
    it("IncomingSchedulerEvent") {
      val bus = new SchedulerEventBus
      val probe1, probe2 = TestProbe()
      bus.subscribe(probe1.ref, classOf[GetJobs])
      bus.subscribe(probe2.ref, classOf[IncomingSchedulerEvent])
      "bus.subscribe(probe.ref, classOf[String])".shouldNot(compile)

      def expect[A <: IncomingSchedulerEvent](event: A) = {
        bus.publish(event)
        probe1.expectMsg(event)
        probe2.expectNoMsg()
      }

      expect(GetJobs(sample[SlackChannel]))
      """bus.publish("a")""".shouldNot(compile)
    }
  }
}
