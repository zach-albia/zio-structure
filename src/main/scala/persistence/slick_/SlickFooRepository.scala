package persistence.slick_

import domain._
import persistence.slick_.Foos.foos
import slick.jdbc.H2Profile.api._
import zio.UIO

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

trait SlickFooRepository extends FooRepository.Service {

  val slickDatabase: SlickDatabase
  implicit val ec: ExecutionContext

  import SlickFooRepository._

  implicit def toZIO[A](dbio: DBIO[A]): UIO[A] =
    SlickZIO(dbio).provide(slickDatabase).orDie

  def create(name: String): UIO[Foo] =
    ((foos returning foos.map(_.id)) += Foo(IGNORED_PLACEHOLDER, name))
      .map(Foo(_, name))

  def fetch(id: Int): UIO[Option[Foo]] =
    foos.filter(_.id === id).result.headOption

  def update(id: Int, name: String): UIO[Option[Foo]] = {
    val updatedFoo = Foo(id, name)
    foos
      .filter(_.id === id)
      .update(updatedFoo)
      .map[Option[Foo]](i => if (i == 0) None else Some(updatedFoo))
  }

  def delete(id: Int): UIO[Unit] =
    foos.filter(_.id === id).delete.map(_ => ())
}

object SlickFooRepository {
  val IGNORED_PLACEHOLDER = 42069
}
