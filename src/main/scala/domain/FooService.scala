package domain

import cats.Monad
import cats.implicits._
import zio.ZIO

import scala.language.higherKinds

/**
  * If I'm understanding the ZIO program structure convention correctly,
  * `FooService` is a top-tier component accessed directly by `Main` so I guess
  * it doesn't need to have an environment member `fooService` like
  * `FooRepository` does.
  */
object FooService {

  /**
    * The FooService environment type. It consists of the `FooRepository`,
    * `HasFunctionK` to transform an `F[_]` to a `ZIO[S, E, *]`, and a
    * `Transactor` to add transaction support to the F effect.
    *
    * @tparam F repository effect type
    * @tparam S repository environment type
    * @tparam E repository error type
    */
  type Environment[F[_], S, E] = FooRepository[F]
    with HasFunctionK[F, ZIO[S, E, *]]
    with Transactor[F]

  /**
    * Provides methods that implement our example use cases for our `Foo` data
    * type. `createFoo` is a basic example of how to use a repository and
    * `mergeFoos` is a more complex example that makes the "merging of `Foo`s"
    * transactional.
    *
    * @tparam R global environment type bounded to this service's environment
    * @tparam F repository effect type
    * @tparam S repository environment type
    * @tparam E repository error type
    */
  trait Service[R <: Environment[F, S, E], F[_], S, E] {

    /**
      * Creates a foo given its name
      *
      * @param name The name of the `Foo`
      * @return The newly created `Foo`
      */
    def createFoo(name: String): ZIO[S with Environment[F, S, E], E, Foo] =
      for {
        env <- ZIO.environment[Environment[F, S, E]]
        foo <- env.functionK(env.fooRepository.create(name))
      } yield foo

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
    def mergeFoos(fooId: Int, otherId: Int)(implicit MonadF: Monad[F])
      : ZIO[S with Environment[F, S, E], E, Option[(Foo, Foo)]] =
      for {
        env         <- ZIO.environment[Environment[F, S, E]]
        foosOpt     <- findFoos(fooId, otherId)
        mergeResult <- ZIO.sequence(doMergeFoos(env, foosOpt))
        mergedFoos  = mergeResult.flatten.sequence.flatMap(pairFoos)
      } yield mergedFoos

    private def doMergeFoos(
        env: Environment[F, S, E],
        foosOpt: Option[List[Foo]])(implicit MonadF: Monad[F]) = {
      for {
        foos         <- foosOpt
        fooBar       <- pairFoos(foos)
        (_1st, _2nd) = fooBar
        mergeF = for {
          foo <- env.fooRepository.update(_1st.id, _1st.name + " " + _2nd.name)
          bar <- env.fooRepository.update(_2nd.id, _2nd.name + " " + _1st.name)
        } yield List(foo, bar)
      } yield env.functionK(env.transactor.transact(mergeF))
    }

    private def findFoos(fooId: Int, otherId: Int) = {
      for {
        env <- ZIO.environment[Environment[F, S, E]]
        fetches = List(
          env.functionK(env.fooRepository.fetch(fooId)),
          env.functionK(env.fooRepository.fetch(otherId))
        )
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
