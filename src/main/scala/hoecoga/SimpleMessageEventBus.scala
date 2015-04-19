package hoecoga

import akka.actor.ActorRef
import akka.event.{EventBus, LookupClassification}
import hoecoga.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.slack.SlackChannel

class SimpleMessageEventBus extends EventBus with LookupClassification {
  override type Event = SimpleMessageEvent
  override type Classifier = Class[_ <: SimpleMessageEvent]
  override type Subscriber = ActorRef

  override protected def mapSize(): Int = 128

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  override protected def classify(event: Event): Classifier = event.getClass

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)
}

object SimpleMessageEventBus {
  case class SimpleMessageEvent(channel: SlackChannel, text: String)
}
