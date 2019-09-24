package domain

import cats.Monad
import cats.arrow.FunctionK
import cats.implicits._
import zio.ZIO

import scala.language.{higherKinds, implicitConversions}

/**
  * If I'm understanding the ZIO program structure convention correctly,
  * `FooService` is a top-tier component accessed directly by `Main` so I guess
  * it doesn't need to have an environment member `fooService` like
  * `FooRepository` does.
  */
object FooService {

  /**
    * Provides methods that implement our example use cases for our `Foo` data
    * type. `createFoo` is a basic example of how to use a repository and
    * `mergeFoos` is a more complex example that makes the "merging of `Foo`s"
    * transactional.
    *
    * @tparam F repository effect type
    */
  trait Service[F[_]] {

    val fooRepository: FooRepository.Service[F]
    val toZIO: FunctionK[F, ZIO[Any, Nothing, *]]
    val transact: Transactor.Service[F]

    /**
      * Creates a foo given its name
      *
      * @param name The name of the `Foo`
      * @return The newly created `Foo`
      */
    def createFoo(name: String): ZIO[Any, Nothing, Foo] =
      toZIO(fooRepository.create(name))

    /**
      * Merges the names of two `Foo` instances identified by their IDs. Merging
      * here simply means the second `Foo`'s name is appended to the first's, and
      * vice versa.
      *
      * @param fooId First `Foo` ID
      * @param otherId Second `Foo` ID
      * @param MonadF Monad instance required for flat-mapping over the
      *               repository effect type `F`.
      * @return The merged `Foo`s as an optional pair
      */
    def mergeFoos(fooId: Int, otherId: Int)(
        implicit MonadF: Monad[F]): ZIO[Any, Nothing, Option[(Foo, Foo)]] =
      for {
        foosOpt     <- findFoos(fooId, otherId)
        mergeResult <- ZIO.sequence(doMergeFoos(foosOpt))
        mergedFoos  = mergeResult.flatten.sequence.flatMap(pairFoos)
      } yield mergedFoos

    private def doMergeFoos(foosOpt: Option[List[Foo]])(
        implicit MonadF: Monad[F]) = {
      for {
        foos         <- foosOpt
        fooBar       <- pairFoos(foos)
        (_1st, _2nd) = fooBar
        mergeF = for {
          foo <- fooRepository.update(_1st.id, _1st.name + " " + _2nd.name)
          bar <- fooRepository.update(_2nd.id, _2nd.name + " " + _1st.name)
        } yield List(foo, bar)
      } yield toZIO(transact(mergeF))
    }

    private def findFoos(fooId: Int, otherId: Int) = {
      val fetches = List(
        fooRepository.fetch(fooId),
        fooRepository.fetch(otherId)
      ).map(toZIO(_))
      for {
        foos <- ZIO.sequence(fetches).map(_.sequence)
      } yield foos
    }

    /** Pairs foos iff the list has two elements */
    private def pairFoos(foos: List[Foo]) = {
      if (foos.length == 2) {
        Some((foos.head, foos.tail.head))
      } else None
    }
  }
}
