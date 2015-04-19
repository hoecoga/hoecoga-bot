package hoecoga

import java.util.{Properties, TimeZone, UUID}

import akka.actor._
import com.google.inject.Injector
import hoecoga.SchedulerActor._
import hoecoga.SchedulerEventBus.{IncomingSchedulerEvent, OutgoingSchedulerEvent}
import hoecoga.SimpleMessageEventBus.SimpleMessageEvent
import hoecoga.scheduler.JobPersistentActor.{Delete, Insert, Query, QueryResult}
import hoecoga.scheduler._
import hoecoga.slack.SlackChannel
import org.quartz.{Scheduler, _}
import org.quartz.impl.StdSchedulerFactory
import play.api.libs.json.Json

import scala.util.control.NonFatal

/**
 * A quartz scheduler actor.
 */
class SchedulerActor(settings: SchedulerActorSettings) extends Actor with ActorLogging {
  import settings._

  private[this] var persistent: ActorRef = null

  private[this] var scheduler: Scheduler = null

  private[this] def register(data: JobData): Unit = {
    val schedule = CronScheduleBuilder.cronSchedule(data.cron).inTimeZone(timeZone)
    val trigger = TriggerBuilder.newTrigger().
      withIdentity(data.id, data.channel.id).withSchedule(schedule).build()
    val job = JobBuilder.newJob(classOf[SlackJob]).withIdentity(data.id, data.channel.id).
      usingJobData(SlackJob.jobDataKey, Json.toJson(data).toString()).build()
    scheduler.scheduleJob(job, trigger)
  }

  override def preStart(): Unit = {
    super.preStart()

    val prop = new Properties()
    prop.setProperty("org.quartz.scheduler.instanceName", UUID.randomUUID().toString)
    prop.setProperty("org.quartz.threadPool.threadCount", threadCount.toString)
    prop.setProperty("org.quartz.jobStore.class", jobStore)

    val factory = new StdSchedulerFactory(prop)
    scheduler = factory.getScheduler
    scheduler.setJobFactory(new SlackJobFactory(injector))
    scheduler.start()

    Seq(classOf[GetJobs], classOf[CreateJob], classOf[DeleteJob]).foreach(bus.subscribe(self, _))

    val persistentSettings = JobPersistentActor.JobPersistentActorSettings(
      persistentId,
      {jobs => jobs.foreach(register)},
      {() => ???})
    persistent = context.actorOf(JobPersistentActor.props(persistentSettings))

    log.info("scheduler started")
  }

  override def postStop(): Unit = {
    super.postStop()

    scheduler.shutdown(true)
    scheduler = null

    bus.unsubscribe(self)

    log.info("scheduler terminated")
  }

  override def receive: Receive = {
    case g @ GetJobs(channel) =>
      log.debug(s"""$g""")
      persistent ! Query(channel)

    case QueryResult(channel, jobs) =>
      bus.publish(Jobs(channel, jobs))

    case c @ CreateJob(channel, cron, message) =>
      log.debug(s"""$c""")
      try {
        val id = newJobId()
        val data = JobData(channel = channel, id = id, cron = cron, message = message)
        register(data)
        persistent ! Insert(data)
        bus.publish(CreatedJob(channel, data))
      } catch {
        case NonFatal(e) =>
          bus.publish(SchedulerFailure(channel, e))
      }

    case d @ DeleteJob(channel, id) =>
      log.debug(s"""$d""")
      try {
        val result = scheduler.deleteJob(JobKey.jobKey(id, channel.id))
        persistent ! Delete(channel, id)
        bus.publish(DeletedJob(channel, id, result))
      } catch {
        case NonFatal(e) =>
          bus.publish(SchedulerFailure(channel, e))
      }
  }
}

object SchedulerActor {
  private val threadCount = 1
  private val jobStore = "org.quartz.simpl.RAMJobStore"
  private val persistentId = "quartz"

  case class GetJobs(override val channel: SlackChannel) extends IncomingSchedulerEvent

  case class Jobs(override val channel: SlackChannel, jobs: List[JobData]) extends OutgoingSchedulerEvent {
    override def simpleMessageEvent: SimpleMessageEvent = {
      val lines = s"${jobs.size} job(s) found" ::
        jobs.map(job => s"id=${job.id}, trigger=${job.cron}, data=${job.message}")
      SimpleMessageEvent(channel, lines.mkString("\n"))
    }
  }

  case class CreateJob(override val channel: SlackChannel, cron: String, message: String) extends IncomingSchedulerEvent

  case class CreatedJob(override val channel: SlackChannel, job: JobData) extends OutgoingSchedulerEvent {
    override def simpleMessageEvent: SimpleMessageEvent = SimpleMessageEvent(channel, s"job ${job.id} created")
  }

  case class DeleteJob(override val channel: SlackChannel, id: String) extends IncomingSchedulerEvent

  case class DeletedJob(override val channel: SlackChannel, id: String, result: Boolean) extends OutgoingSchedulerEvent {
    override def simpleMessageEvent: SimpleMessageEvent = {
      val text = if (result) s"job $id deleted" else s"job $id not found"
      SimpleMessageEvent(channel, text)
    }
  }

  case class SchedulerFailure(override val channel: SlackChannel, e: Throwable) extends OutgoingSchedulerEvent {
    override def simpleMessageEvent: SimpleMessageEvent = SimpleMessageEvent(channel, e.getMessage)
  }

  /**
   * @param injector refer to [[SlackJobFactory]].
   * @param timeZone the scheduler timezone.
   */
  case class SchedulerActorSettings(newJobId: () => String, injector: Injector, timeZone: TimeZone, bus: SchedulerEventBus)

  def props(settings: SchedulerActorSettings): Props = Props(new SchedulerActor(settings))
}
