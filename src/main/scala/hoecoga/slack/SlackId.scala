package hoecoga.slack

import org.scalacheck.Gen

object SlackId {
  def gen(c: Char): Gen[String] = Gen.listOfN(8, Gen.oneOf(Gen.numChar, Gen.alphaUpperChar)).map(p => (c :: p).mkString)
}
