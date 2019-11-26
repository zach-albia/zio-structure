package persistence.slick_

import domain._
import slick.jdbc.H2Profile.api._
import zio.ZIO

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

/**
  * If I'm understanding the ZIO program structure convention correctly,
  * `FooService` is a top-tier component accessed directly by `Main` so I guess
  * it doesn't need to have an environment member `fooService` like
  * `FooRepository` does.
  */
object SlickFooService {

  /**
    * Provides methods that implement our example use cases for our `Foo` data
    * type. `createFoo` is a basic example of how to use a repository and
    * `mergeFoos` is a more complex example that makes the "merging of `Foo`s"
    * transactional.
    */
  trait Service extends FooService.Service {

    val db: Database
    val slickDb: SlickDatabase = new SlickDatabase { val database = db }
    val repo: FooRepository.Service[DBIO]
    implicit val ec: ExecutionContext

    implicit def toZIO[A](dbio: DBIO[A]): ZIO[Any, Nothing, A] =
      SlickZIO(dbio).provide(slickDb).orDie

    /**
      * Creates a foo given its name
      *
      * @param name The name of the `Foo`
      * @return The newly created `Foo`
      */
    def createFoo(name: String): ZIO[Any, Nothing, Foo] =
      repo.create(name)

    /**
      * Merges the names of two `Foo` instances identified by their IDs. Merging
      * here simply means the second `Foo`'s name is appended to the first's, and
      * vice versa.
      *
      * @param fooId First `Foo` ID
      * @param otherId Second `Foo` ID
      * @return The merged `Foo`s as an optional pair
      */
    def mergeFoos(fooId: Int,
                  otherId: Int): ZIO[Any, Nothing, Option[(Foo, Foo)]] =
      for {
        foosOpt     <- findFoos(fooId, otherId)
        mergeResult <- doMergeFoos(foosOpt)
      } yield mergeResult

    private def findFoos(fooId: Int, otherId: Int) =
      toZIO(for {
        fooPair            <- repo.fetch(fooId).zip(repo.fetch(otherId))
        (fooOpt, otherOpt) = fooPair
        opt = for {
          foo   <- fooOpt
          other <- otherOpt
        } yield (foo, other)
      } yield opt)

    private def doMergeFoos(
        foosOpt: Option[(Foo, Foo)]
    ): ZIO[Any, Nothing, Option[(Foo, Foo)]] = {
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
        .getOrElse(DBIO.successful(None))
        .transactionally
    }
  }
}
