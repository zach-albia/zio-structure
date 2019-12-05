package domain

import zio._

trait FooRepository {
  val fooRepository: FooRepository.Service
}

object FooRepository {

  trait Service {
    def create[R](name: String): URIO[R, Foo]

    def fetch[R](id: Int): URIO[R, Option[Foo]]

    def update[R](id: Int, name: String): URIO[R, Option[Foo]]

    def delete[R](id: Int): URIO[R, Unit]
  }

  case class InMemoryRepository(mapTable: Ref[Map[Int, Foo]],
                                idSequence: Ref[Int])
      extends FooRepository.Service {
    def create[Any](name: String): UIO[Foo] =
      for {
        newId <- idSequence.update(_ + 1)
        foo   = Foo(newId, name)
        _     <- mapTable.update(store => store + (newId -> foo))
      } yield foo

    def fetch[Any](id: Int): UIO[Option[Foo]] =
      mapTable.get.map(_.get(id))

    def update[Any](id: Int, name: String): UIO[Option[Foo]] =
      for {
        updatedFooOpt <- mapTable.get.map(_.get(id).map(_ => Foo(id, name)))
        _ <- updatedFooOpt
              .map(foo => mapTable.update(store => store + (id -> foo)))
              .getOrElse(UIO.unit)
      } yield updatedFooOpt

    def delete[Any](id: Int): UIO[Unit] =
      mapTable.update(_ - id).unit
  }
}
