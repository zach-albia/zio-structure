package persistence

import slick.basic.BasicBackend
import slick.dbio.DBIO
import zio.ZIO

import scala.language.implicitConversions

package object slick_ {
  trait SlickDatabase {
    val database: BasicBackend#DatabaseDef
  }

  object SlickDatabase {

    object Live {
      def apply(db: BasicBackend#DatabaseDef) = new SlickDatabase {
        override val database = db
      }
    }
  }

  type SlickZIO[T] = ZIO[SlickDatabase, Throwable, T]

  object SlickZIO {

    def apply[T](action: DBIO[T]): SlickZIO[T] = {
      for {
        database <- ZIO.access[SlickDatabase](_.database)
        res <- ZIO.fromFuture(implicit ec => database.run(action))
      } yield res
    }
  }

  implicit def DBIO2SlickZIO[T](dbio: DBIO[T]): SlickZIO[T] = SlickZIO[T](dbio)
}
