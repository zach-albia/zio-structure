package domain

import zio.{Ref, UIO}

import scala.language.higherKinds

trait FooRepository[F[_]] {

  val fooRepository: FooRepository.Service[F]
}

object FooRepository {

  trait Service[F[_]] {

    def create(name: String): F[Foo]

    def fetch(id: String): F[Option[Foo]]

    def update(id: String, name: String): F[Option[Foo]]

    def delete(id: String): F[Unit]
  }

  final case class InMemoryFooRepository(
      ref: Ref[Map[String, Foo]],
      counter: Ref[Long]
  ) extends Service[UIO] {

    override def create(name: String): UIO[Foo] =
      for {
        newId <- counter.update(_ + 1).map(_.toString)
        foo = Foo(newId, name)
        _ <- ref.update(store => store + (newId -> foo))
      } yield foo

    override def fetch(id: String): UIO[Option[Foo]] =
      ref.get.map(_.get(id))

    override def update(id: String, name: String): UIO[Option[Foo]] =
      for {
        updatedFooOpt <- ref.get.map(_.get(id).map(_ => Foo(id, name)))
        _ <- updatedFooOpt
          .map(foo => ref.update(store => store + (id -> foo)))
          .getOrElse(UIO.unit)
      } yield updatedFooOpt

    override def delete(id: String): UIO[Unit] =
      ref.update(_ - id).unit
  }

}
