package hoecoga.slack

import hoecoga.core.JsonUtil
import play.api.libs.json.Format

/**
 * @see [[https://api.slack.com/rtm]]
 */
case class PongEvent(replyTo: Long, tpe: String = "pong") {
  require(tpe == "pong")
}

object PongEvent {
  implicit val format: Format[PongEvent] = JsonUtil.format("reply_to", "type", PongEvent.apply, PongEvent.unapply)
}
