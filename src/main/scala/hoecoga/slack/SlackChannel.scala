package hoecoga.slack

import org.scalacheck.Arbitrary
import play.api.libs.json.{Format, JsString, Reads, Writes}

/**
 * A slack channel.
 * @param id the slack channel identifier.
 */
case class SlackChannel(id: String)

object SlackChannel {
  implicit val format: Format[SlackChannel] =
    Format(Reads(_.validate[String].map(SlackChannel.apply)), Writes(c => JsString(c.id)))

  implicit val arbitrary: Arbitrary[SlackChannel] = Arbitrary(SlackId.gen('C').map(SlackChannel.apply))
}
