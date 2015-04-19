package hoecoga.core

import org.scalacheck.Arbitrary

trait ArbitraryUtil {
  def arbitrary[A, B, C](apply: (A, B) => C)(implicit aa: Arbitrary[A], ab: Arbitrary[B]): Arbitrary[C] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary
      } yield (ga, gb)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D](apply: (A, B, C) => D)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C]): Arbitrary[D] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary
      } yield (ga, gb, gc)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E](apply: (A, B, C, D) => E)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D]): Arbitrary[E] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary
      } yield (ga, gb, gc, gd)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E, F](apply: (A, B, C, D, E) => F)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E]): Arbitrary[F] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary; ge <- ae.arbitrary
      } yield (ga, gb, gc, gd, ge)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E, F, G](apply: (A, B, C, D, E, F) => G)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E], af: Arbitrary[F]): Arbitrary[G] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary; ge <- ae.arbitrary; gf <- af.arbitrary
      } yield (ga, gb, gc, gd, ge, gf)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E, F, G, H](apply: (A, B, C, D, E, F, G) => H)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E], af: Arbitrary[F], ag: Arbitrary[G]): Arbitrary[H] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary; ge <- ae.arbitrary; gf <- af.arbitrary; gg <- ag.arbitrary
      } yield (ga, gb, gc, gd, ge, gf, gg)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E, F, G, H, I](apply: (A, B, C, D, E, F, G, H) => I)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E], af: Arbitrary[F], ag: Arbitrary[G], ah: Arbitrary[H]): Arbitrary[I] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary; ge <- ae.arbitrary; gf <- af.arbitrary; gg <- ag.arbitrary; gh <- ah.arbitrary
      } yield (ga, gb, gc, gd, ge, gf, gg, gh)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E, F, G, H, I, J](apply: (A, B, C, D, E, F, G, H, I) => J)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E], af: Arbitrary[F], ag: Arbitrary[G], ah: Arbitrary[H], ai: Arbitrary[I]): Arbitrary[J] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary; ge <- ae.arbitrary; gf <- af.arbitrary; gg <- ag.arbitrary; gh <- ah.arbitrary; gi <- ai.arbitrary
      } yield (ga, gb, gc, gd, ge, gf, gg, gh, gi)
      tuple.map(apply.tupled)
    }
  }

  def arbitrary[A, B, C, D, E, F, G, H, I, J, K](apply: (A, B, C, D, E, F, G, H, I, J) => K)(implicit aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E], af: Arbitrary[F], ag: Arbitrary[G], ah: Arbitrary[H], ai: Arbitrary[I], aj: Arbitrary[J]): Arbitrary[K] = {
    Arbitrary {
      val tuple = for {
        ga <- aa.arbitrary; gb <- ab.arbitrary; gc <- ac.arbitrary; gd <- ad.arbitrary; ge <- ae.arbitrary; gf <- af.arbitrary; gg <- ag.arbitrary; gh <- ah.arbitrary; gi <- ai.arbitrary; gj <- aj.arbitrary
      } yield (ga, gb, gc, gd, ge, gf, gg, gh, gi, gj)
      tuple.map(apply.tupled)
    }
  }

}

object ArbitraryUtil extends ArbitraryUtil
