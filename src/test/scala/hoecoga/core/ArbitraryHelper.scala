package hoecoga.core

import org.scalacheck.Arbitrary

trait ArbitraryHelper {
  def sample[A](implicit a: Arbitrary[A]): A = Stream.continually(a.arbitrary.sample).filter(_.isDefined).flatten.head
}
