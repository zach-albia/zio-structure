package persistence.slick_

import cats.arrow.FunctionK
import slick.jdbc.H2Profile.api._
import zio.UIO

trait SlickFunctionK extends FunctionK[DBIO, UIO] {
  val db: Database

  override def apply[A](dbioA: DBIO[A]): UIO[A] =
    SlickZIO(dbioA).provide(new SlickDatabase { val database = db }).orDie
}
