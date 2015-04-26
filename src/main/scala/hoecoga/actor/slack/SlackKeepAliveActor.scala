package hoecoga.actor.slack

import java.time.LocalDateTime

import akka.actor._
import hoecoga.actor.slack.SlackKeepAliveActor._
import hoecoga.slack.PongEvent

import scala.concurrent.duration.FiniteDuration

class SlackKeepAliveActor(settings: SlackKeepAliveActorSettings) extends Actor with ActorLogging {
  import settings._
  import context.dispatcher

  private[this] def now() = LocalDateTime.now()

  private[this] var scheduler: Cancellable = null

  private[this] var activity = now()

  override def preStart(): Unit = {
    super.preStart()
    scheduler = context.system.scheduler.schedule(interval, interval, self, Tick)
  }

  override def postStop(): Unit = {
    super.postStop()

    if (scheduler != null) scheduler.cancel()
  }

  private[this] def await: Receive = {
    case Tick =>
      if (now().isAfter(activity.plusSeconds(interval.toSeconds))) {
        actor ! ExceedInterval
      }

    case PongEvent(_, _) | Alive =>
      activity = now()
      context.unbecome()
  }
  
  override def receive: Receive = {
    case Tick =>
      if (now().isAfter(activity.plusSeconds(interval.toSeconds))) {
        actor ! AskKeepAlive
        context.become(await)
      }

    case Alive =>
      activity = now()
  }
}

object SlackKeepAliveActor {
  private case object Tick

  /**
   * An outgoing message to send a keep-alive message.
   */
  case object AskKeepAlive

  /**
   * An incoming message which indicates the websocket connection is still alive.
   */
  case object Alive

  /**
   * An outgoing message to close the websocket connection.
   */
  case object ExceedInterval

  case class SlackKeepAliveActorSettings(actor: ActorRef, interval: FiniteDuration)

  def props(settings: SlackKeepAliveActorSettings): Props = Props(new SlackKeepAliveActor(settings))
}
