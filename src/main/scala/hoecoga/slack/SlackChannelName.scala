package hoecoga.slack

import org.scalacheck.{Gen, Arbitrary}

/**
 * A slack channel name.
 * @param name #name.
 */
case class SlackChannelName(name: String)

object SlackChannelName {
  implicit val arbitrary: Arbitrary[SlackChannelName] = Arbitrary(Gen.identifier.map(SlackChannelName.apply))
}
