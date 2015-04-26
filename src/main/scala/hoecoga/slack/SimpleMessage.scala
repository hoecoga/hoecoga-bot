package hoecoga.slack

import hoecoga.core.JsonUtil
import play.api.libs.json.Format

/**
 * @see [[https://api.slack.com/rtm]]
 */
case class SimpleMessage(id: Long, channel: SlackChannel, text: String, tpe: String = "message") {
  require(tpe == "message")
}

object SimpleMessage {
  implicit val format: Format[SimpleMessage] =
    JsonUtil.format("id", "channel", "text", "type", SimpleMessage.apply, SimpleMessage.unapply)
}
