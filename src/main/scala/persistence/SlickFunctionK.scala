package persistence

import cats.arrow.FunctionK
import slick.jdbc.H2Profile.api._

object SlickFunctionK extends FunctionK[DBIO, SlickZIO] {

  override def apply[A](dbioA: DBIO[A]): SlickZIO[A] =
    SlickZIO(dbioA)
}
