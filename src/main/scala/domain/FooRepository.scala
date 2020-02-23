package domain

import domain.SaveError.ThrowableError
import zio._

import scala.language.higherKinds

object FooRepository {

  trait Service {

    def create(name: String): IO[SaveError, Foo]

    def fetch(id: Int): IO[ThrowableError, Option[Foo]]

    def update(id: Int, name: String): IO[SaveError, Option[Foo]]

    def delete(id: Int): IO[ThrowableError, Unit]
  }

  // Still don't know what this is for...
  val any: ZLayer[FooRepository, Nothing, FooRepository] =
    ZLayer.requires[FooRepository]

  val inMem: ZLayer[Any, Nothing, FooRepository] =
    ZLayer.fromEffect(for {
      map        <- Ref.make(Map.empty[Int, Foo])
      idSequence <- Ref.make(0)
    } yield InMemoryFooRepository(map, idSequence))

  //<editor-fold desc="zio-macros should replace these">
  def create(name: String): ZIO[FooRepository, SaveError, Foo] =
    ZIO.accessM(_.get.create(name))

  def fetch(id: Int): ZIO[FooRepository, ThrowableError, Option[Foo]] =
    ZIO.accessM(_.get.fetch(id))

  def update(id: Int, name: String): ZIO[FooRepository, SaveError, Option[Foo]] =
    ZIO.accessM(_.get.update(id, name))
  //</editor-fold>

  final case class InMemoryFooRepository(
      mapTable: Ref[Map[Int, Foo]],
      idSequence: Ref[Int]
  ) extends Service {

    def create(name: String): UIO[Foo] =
      for {
        newId <- idSequence.updateAndGet(_ + 1)
        foo   = Foo(newId, name)
        _     <- mapTable.update(store => store + (newId -> foo))
      } yield foo

    def fetch(id: Int): UIO[Option[Foo]] =
      mapTable.get.map(_.get(id))

    def update(id: Int, name: String): UIO[Option[Foo]] =
      for {
        updatedFooOpt <- mapTable.get.map(_.get(id).map(_ => Foo(id, name)))
        _ <- updatedFooOpt
              .map(foo => mapTable.update(store => store + (id -> foo)))
              .getOrElse(UIO.unit)
      } yield updatedFooOpt

    def delete(id: Int): UIO[Unit] =
      mapTable.update(_ - id).unit
  }

}
