package hoecoga.slack

import hoecoga.core.JsonUtil
import play.api.libs.json.Format

/**
 * @see [[https://api.slack.com/rtm]]
 */
case class PingMessage(id: Long, tpe: String = "ping") {
  require(tpe == "ping")
}

object PingMessage {
  implicit val format: Format[PingMessage] = JsonUtil.format("id", "type", PingMessage.apply, PingMessage.unapply)
}
