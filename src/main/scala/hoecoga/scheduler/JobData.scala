package hoecoga.scheduler

import hoecoga.core.{ArbitraryUtil, JsonUtil}
import hoecoga.slack.SlackChannel
import org.scalacheck.Arbitrary
import play.api.libs.json.Format

/**
 * A simple message job description.
 * @param channel a job created on the slack channel.
 * @param id the job identifier.
 * @param cron the job trigger.
 * @param message a job sends the simple message to the slack channel.
 */
case class JobData(channel: SlackChannel, id: String, cron: String, message: String)

object JobData {
  implicit val format: Format[JobData] = JsonUtil.format("channel", "id", "cron", "data", JobData.apply, JobData.unapply)

  implicit val arbitrary: Arbitrary[JobData] = ArbitraryUtil.arbitrary(JobData.apply _)
}
