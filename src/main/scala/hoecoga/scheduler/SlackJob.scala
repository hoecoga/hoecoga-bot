package hoecoga.scheduler

import org.quartz.{Job, JobExecutionContext}
import play.api.libs.json.Json

/**
 * A job to send a simple message to slack.
 */
trait SlackJob extends Job {
  protected def tell(job: JobData): Unit

  override def execute(context: JobExecutionContext): Unit = {
    val data = Json.parse(context.getMergedJobDataMap.getString(SlackJob.jobDataKey)).as[JobData]
    tell(data)
  }
}

object SlackJob {
  /**
   * The quartz job data map key to set/get [[JobData]] JSON string.
   */
  val jobDataKey = "data"
}
