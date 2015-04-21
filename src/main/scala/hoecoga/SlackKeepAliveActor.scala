package hoecoga

import java.time.LocalDateTime

import akka.actor._
import hoecoga.SlackKeepAliveActor._
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

  case object AskKeepAlive

  case object Alive

  case object ExceedInterval

  case class SlackKeepAliveActorSettings(actor: ActorRef, interval: FiniteDuration)

  def props(settings: SlackKeepAliveActorSettings): Props = Props(new SlackKeepAliveActor(settings))
}
