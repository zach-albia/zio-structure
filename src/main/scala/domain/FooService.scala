package domain

import zio._

trait FooService {
  val fooService: FooService.Service
}

object FooService {

  trait Service[R] extends FooRepository {

    import fooRepository._

    def transactor[, A](program: ZIO[R, Nothing, A]): ZIO[R, Nothing, A]

    def createFoo[R](name: String): URIO[R, Foo] =
      create[R](name)

    def mergeFoos[R](fooId: Int, otherId: Int): URIO[R, Option[(Foo, Foo)]] =
      for {
        foosOpt     <- findFoos[R](fooId, otherId)
        mergeResult <- doMergeFoos(foosOpt)
      } yield mergeResult

    def findFoos[R](fooId: Int, otherId: Int) =
      for {
        fooPair            <- fetch[R](fooId).zip(fetch(otherId))
        (fooOpt, otherOpt) = fooPair
        opt = for {
          foo   <- fooOpt
          other <- otherOpt
        } yield (foo, other)
      } yield opt

    def doMergeFoos[R](
        foosOpt: Option[(Foo, Foo)]): URIO[R, Option[(Foo, Foo)]] =
      transactor(
        foosOpt
          .map({
            case (foo, other) =>
              val mergedFooName   = foo.name + " " + other.name
              val mergedOtherName = other.name + " " + foo.name
              for {
                fooOpt   <- update[R](foo.id, mergedFooName)
                otherOpt <- update[R](other.id, mergedOtherName)
                fooOther = for {
                  foo   <- fooOpt
                  other <- otherOpt
                } yield (foo, other)
              } yield fooOther
          })
          .getOrElse(URIO.succeed(None)))
  }

  case class InMemoryFooService(map: Ref[Map[Int, Foo]], counter: Ref[Int])
      extends Service {

    def transactor[R, A](program: ZIO[R, Nothing, A]): ZIO[R, Nothing, A] =
      program

    val fooRepository: FooRepository.Service =
      FooRepository.InMemoryRepository(map, counter)
  }
}
