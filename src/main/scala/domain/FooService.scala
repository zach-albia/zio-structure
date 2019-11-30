package domain

import zio._

trait FooService {
  val fooService: FooService.Service
}

object FooService {
  trait Service {
    def createFoo(name: String): ZIO[Any, Nothing, Foo]

    def mergeFoos(fooId: Int,
                  otherId: Int): ZIO[Any, Nothing, Option[(Foo, Foo)]]
  }

  case class InMemoryFooService(mapTable: Ref[Map[Int, Foo]],
                                idSequence: Ref[Int]) extends Service {

    override def createFoo(name: String): ZIO[Any, Nothing, Foo] =
      for {
        newId <- idSequence.update(_ + 1)
        foo   = Foo(newId, name)
        _     <- mapTable.update(store => store + (newId -> foo))
      } yield foo

    override def mergeFoos(
        fooId: Int,
        otherId: Int): ZIO[Any, Nothing, Option[(Foo, Foo)]] =
      for {
        foosOpt     <- findFoos(fooId, otherId)
        mergeResult <- doMergeFoos(foosOpt)
      } yield mergeResult

    def findFoos(fooId: Int, otherId: Int) =
      for {
        fooPair            <- fetch(fooId).zip(fetch(otherId))
        (fooOpt, otherOpt) = fooPair
        opt = for {
          foo   <- fooOpt
          other <- otherOpt
        } yield (foo, other)
      } yield opt

    def doMergeFoos(foosOpt: Option[(Foo, Foo)]) =
      foosOpt
        .map {
          case (foo, other) =>
            val mergedFooName   = foo.name + " " + other.name
            val mergedOtherName = other.name + " " + foo.name
            for {
              fooOpt   <- update(foo.id, mergedFooName)
              otherOpt <- update(other.id, mergedOtherName)
              fooOther = for {
                foo   <- fooOpt
                other <- otherOpt
              } yield (foo, other)
            } yield fooOther
        }
        .getOrElse(ZIO.succeed(None))

    private def fetch(id: Int): UIO[Option[Foo]] =
      mapTable.get.map(_.get(id))

    private def update(id: Int, name: String): UIO[Option[Foo]] =
      for {
        updatedFooOpt <- mapTable.get.map(_.get(id).map(_ => Foo(id, name)))
        _ <- updatedFooOpt
          .map(foo => mapTable.update(store => store + (id -> foo)))
          .getOrElse(UIO.unit)
      } yield updatedFooOpt

    private def delete(id: Int): UIO[Unit] =
      mapTable.update(_ - id).unit
  }
}
