package hoecoga.core

import org.scalacheck.Arbitrary

trait ArbitraryHelper { self =>
  def sample[A](implicit a: Arbitrary[A]): A = Stream.continually(a.arbitrary.sample).filter(_.isDefined).flatten.head

  class Unique[A](implicit a: Arbitrary[A]) {
    private[this] val set: scala.collection.mutable.Set[A] = scala.collection.mutable.Set.empty

    def sample(): A = synchronized {
      val unique = Stream.continually(self.sample).filter(a => !set.contains(a)).head
      set.add(unique)
      unique
    }
  }
}
