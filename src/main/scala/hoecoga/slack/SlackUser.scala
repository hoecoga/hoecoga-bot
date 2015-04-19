package hoecoga.slack

import org.scalacheck.Arbitrary
import play.api.libs.json.{Format, JsString, Reads, Writes}

/**
 * A slack user.
 * @param id the slack user identifier.
 */
case class SlackUser(id: String)

object SlackUser {
  implicit val format: Format[SlackUser] =
    Format(Reads(_.validate[String].map(SlackUser.apply)), Writes(c => JsString(c.id)))

  implicit val arbitrary: Arbitrary[SlackUser] = Arbitrary(SlackId.gen('U').map(SlackUser.apply))
}
