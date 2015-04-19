package hoecoga.slack

import hoecoga.core.{ArbitraryUtil, JsonUtil}
import org.scalacheck.Arbitrary
import play.api.libs.json.Format

/**
 * @see [[https://api.slack.com/events/message]]
 */
case class MessageEvent(channel: SlackChannel, user: SlackUser, text: String, ts: String)

object MessageEvent {
  implicit val format: Format[MessageEvent] =
    JsonUtil.format("channel", "user", "text", "ts", MessageEvent.apply, MessageEvent.unapply)

  implicit val arbitrary: Arbitrary[MessageEvent] = ArbitraryUtil.arbitrary(MessageEvent.apply _)
}
