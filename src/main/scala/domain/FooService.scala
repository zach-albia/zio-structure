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

    // just for convenience
    type EnvBound = Environment[F, S, E]

    /**
      * Creates a foo given its name
      *
      * @param name The name of the `Foo`
      * @return The newly created `Foo`
      */
    def createFoo(name: String): ZIO[S with EnvBound, E, Foo] =
      for {
        env <- ZIO.environment[EnvBound]
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
    def mergeFoos(fooId: String, otherId: String)(implicit MonadF: Monad[F])
      : ZIO[S with EnvBound, E, Option[(Foo, Foo)]] =
      for {
        env <- ZIO.environment[EnvBound]
        foosOpt <- ZIO
          .sequence(
            List(
              env.functionK(env.fooRepository.fetch(fooId)),
              env.functionK(env.fooRepository.fetch(otherId))
            ))
          .map(_.sequence)
        resOpt = for {
          foos <- foosOpt
          fooPair <- foos.headOption.zip(foos.tail.headOption)
          fa = for {
            foo <- env.fooRepository
              .update(fooId, fooPair._1.name + " " + fooPair._2.name)
            other <- env.fooRepository
              .update(otherId, fooPair._2.name + " " + fooPair._1.name)
          } yield List(foo, other)
          res = env.functionK(env.transactor.transact(fa))
        } yield res
        result <- ZIO.sequence(resOpt)
        mergedFoos = result.flatten.sequence
          .flatMap(l => l.headOption.zip(l.tail.headOption))
      } yield mergedFoos
  }
}
