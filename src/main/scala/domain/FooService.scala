package domain

import cats.Monad
import cats.arrow.FunctionK
import cats.implicits._
import zio.ZIO

import scala.language.{higherKinds, implicitConversions}

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
        fooBar      <- findFoos(fooId, otherId)
        mergeResult <- doMergeFoos(fooBar)
      } yield mergeResult

    private def findFoos(fooId: Int, otherId: Int) = {
      val foo = toZIO(fooRepository.fetch(fooId))
      val bar = toZIO(fooRepository.fetch(otherId))
      foo zip bar map {
        case (fooOpt, otherOpt) =>
          for {
            foo   <- fooOpt
            other <- otherOpt
          } yield (foo, other)
      }
    }

    private def doMergeFoos(fooPairOpt: Option[(Foo, Foo)])(
        implicit MonadF: Monad[F]): ZIO[Any, Nothing, Option[(Foo, Foo)]] = {
      ZIO
        .sequence(for {
          fooPair    <- fooPairOpt
          (foo, bar) = fooPair
          mergeM = for {
            fooOpt <- fooRepository.update(foo.id, foo.name + " " + bar.name)
            barOpt <- fooRepository.update(bar.id, bar.name + " " + foo.name)
          } yield (fooOpt, barOpt).tupled
        } yield toZIO(transact(mergeM)))
        .map(_.headOption.flatten)
    }
  }
}
