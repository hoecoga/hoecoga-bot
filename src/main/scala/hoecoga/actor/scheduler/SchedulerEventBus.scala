package hoecoga.actor.scheduler

import akka.actor.ActorRef
import akka.event.{EventBus, LookupClassification}
import hoecoga.actor.scheduler.SchedulerEventBus.SchedulerEvent
import hoecoga.actor.slack.SimpleMessageEventBus
import SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.slack.SlackChannel

class SchedulerEventBus extends EventBus with LookupClassification {
  override type Event = SchedulerEvent
  override type Classifier = Class[_ <: SchedulerEvent]
  override type Subscriber = ActorRef

  override protected def mapSize(): Int = 128

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  override protected def classify(event: Event): Classifier = event.getClass

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)
}

object SchedulerEventBus {
  trait SchedulerEvent {
    val channel: SlackChannel
  }

  trait IncomingSchedulerEvent extends SchedulerEvent

  trait OutgoingSchedulerEvent extends SchedulerEvent {
    def simpleMessageEvent: SimpleMessageEvent
  }
}
