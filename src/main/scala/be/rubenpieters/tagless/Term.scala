package be.rubenpieters.tagless

import cats.Monad

/**
  * Created by ruben on 23/10/2016.
  */
trait Term[Alg[_[_]], +A] {
  def apply[F[+_]](A: Alg[F]): F[A]
}
