package persistence.slick_

import domain.{Foo, FooRepository}
import persistence.slick_.Foos.foos
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

case class SlickFooRepository(implicit ec: ExecutionContext)
    extends FooRepository.Service[DBIO] {

  import SlickFooRepository._

  def create(name: String): DBIO[Foo] =
    ((foos returning foos.map(_.id)) += Foo(IGNORED_PLACEHOLDER, name))
      .map(Foo(_, name))

  def fetch(id: Int): DBIO[Option[Foo]] =
    foos.filter(_.id === id).result.headOption

  def update(id: Int, name: String): DBIO[Option[Foo]] = {
    val updatedFoo = Foo(id, name)
    foos
      .filter(_.id === id)
      .update(updatedFoo)
      .map[Option[Foo]](i => if (i == 0) None else Some(updatedFoo))
  }

  def delete(id: Int): DBIO[Unit] =
    foos.filter(_.id === id).delete.map(_ => ())
}

object SlickFooRepository {
  val IGNORED_PLACEHOLDER = 42069
}
