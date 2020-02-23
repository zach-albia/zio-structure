package persistence.slick_

import domain.SaveError.ThrowableError
import domain._
import persistence.slick_.Foos.foos
import slick.basic.BasicBackend
import slick.jdbc.H2Profile.api._
import zio.{Has, IO, ZLayer}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object SlickFooRepository {
  val IGNORED_PLACEHOLDER = 42069

  def live
    : ZLayer[SlickDatabase with Has[ExecutionContext], Nothing, FooRepository] =
    // weird how not spelling out the type params here causes compile error
    ZLayer.fromServices[BasicBackend#DatabaseDef,
                        ExecutionContext,
                        FooRepository.Service] {
      (slickDatabase: BasicBackend#DatabaseDef, ec_ : ExecutionContext) =>
        {
          implicit val ec: ExecutionContext = ec_
          val service: FooRepository.Service = new FooRepository.Service {

            implicit def toZIO[A](dbio: DBIO[A]): IO[ThrowableError, A] =
              SlickDatabase
                .run(dbio)
                .provide(Has(slickDatabase))
                .mapError(ThrowableError)

            def create(name: String): IO[ThrowableError, Foo] =
              ((foos returning foos.map(_.id)) += Foo(IGNORED_PLACEHOLDER,
                                                      name))
                .map(Foo(_, name))

            def fetch(id: Int): IO[ThrowableError, Option[Foo]] =
              foos.filter(_.id === id).result.headOption

            def update(id: Int,
                       name: String): IO[ThrowableError, Option[Foo]] = {
              val updatedFoo = Foo(id, name)
              foos
                .filter(_.id === id)
                .update(updatedFoo)
                .map[Option[Foo]](i => if (i == 0) None else Some(updatedFoo))
            }

            def delete(id: Int): IO[ThrowableError, Unit] =
              foos.filter(_.id === id).delete.map(_ => ())
          }
          service
        }
    }

}
