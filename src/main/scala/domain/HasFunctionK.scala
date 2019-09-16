package domain

import cats.arrow.FunctionK

import scala.language.higherKinds

trait HasFunctionK[F[_], G[_]] {

  val functionK: FunctionK[F, G]
}
