package persistence

import domain.{Foo, FooRepository}
import persistence.Foos.foos
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

case class FooRepositorySlick(implicit ec: ExecutionContext)
    extends FooRepository.Service[DBIO] {

  import FooRepositorySlick._

  def create(name: String): DBIO[Foo] =
    (foos returning foos.map(_.id)
      into ((foo, id) => foo.copy(id))) += Foo(IGNORED_PLACEHOLDER, name)

  override def fetch(id: Int): DBIO[Option[Foo]] =
    foos.filter(_.id === id).result.headOption

  override def update(id: Int, name: String): DBIO[Option[Foo]] = {
    val updatedFoo = Foo(id, name)
    foos
      .filter(_.id === id)
      .update(updatedFoo)
      .map[Option[Foo]](i => if (i == 0) None else Some(updatedFoo))
  }

  override def delete(id: Int): DBIO[Unit] =
    foos.filter(_.id === id).delete.map(_ => ())
}

object FooRepositorySlick {
  val IGNORED_PLACEHOLDER = 42069
}
