package domain

import domain.FooRepository.InMemoryFooRepository
import zio.ZIO

trait FooService {
  val fooService: FooService.Service
}

object FooService {
  trait Service {
    def createFoo(name: String): ZIO[Any, Nothing, Foo]

    def mergeFoos(fooId: Int,
                  otherId: Int): ZIO[Any, Nothing, Option[(Foo, Foo)]]
  }

  case class InMemoryFooService(repo: InMemoryFooRepository) extends Service {
    override def createFoo(name: String): ZIO[Any, Nothing, Foo] =
      repo.create(name)

    override def mergeFoos(
        fooId: Int,
        otherId: Int): ZIO[Any, Nothing, Option[(Foo, Foo)]] =
      for {
        foosOpt     <- findFoos(fooId, otherId)
        mergeResult <- doMergeFoos(foosOpt)
      } yield mergeResult

    def findFoos(fooId: Int, otherId: Int) =
      for {
        fooPair            <- repo.fetch(fooId).zip(repo.fetch(otherId))
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
              fooOpt   <- repo.update(foo.id, mergedFooName)
              otherOpt <- repo.update(other.id, mergedOtherName)
              fooOther = for {
                foo   <- fooOpt
                other <- otherOpt
              } yield (foo, other)
            } yield fooOther
        }
        .getOrElse(ZIO.succeed(None))
  }
}
