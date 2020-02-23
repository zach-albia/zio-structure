package persistence

import slick.basic.BasicBackend
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend
import zio.{Has, ZIO, ZLayer}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

package object slick_ {

  type SlickDatabase = Has[BasicBackend#DatabaseDef]
  type SlickZIO[T]   = ZIO[SlickDatabase, Throwable, T]

  def ec(ec: ExecutionContext): ZLayer.NoDeps[Nothing, Has[ExecutionContext]] =
    ZLayer.succeed(ec)

  object SlickDatabase {

    def run[T](action: DBIO[T]): SlickZIO[T] =
      ZIO.accessM[SlickDatabase](s => ZIO.fromFuture(_ => s.get.run(action)))

    def live(config: String): ZLayer.NoDeps[Nothing, SlickDatabase] =
      ZLayer.fromEffect(ZIO.effectTotal(JdbcBackend.Database.forConfig(config)))
  }

  implicit def DBIO2SlickZIO[T](dbio: DBIO[T]): SlickZIO[T] =
    SlickDatabase.run(dbio)
}
