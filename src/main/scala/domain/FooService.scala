package domain

import cats.implicits._
import com.vladkopanev.zio.saga.Saga._
import zio.ZIO

import scala.language.{higherKinds, implicitConversions}

trait FooService {
  val fooService: FooService.Service
}

object FooService {

  /**
    * Provides methods that implement our example use cases for our `Foo` data
    * type. `createFoo` is a basic example of how to use a repository and
    * `mergeFoos` is a more complex example that makes the "merging of `Foo`s"
    * transactional.
    */
  trait Service {

    val fooRepository: FooRepository.Service

    /**
      * Creates a foo given its name
      *
      * @param name The name of the `Foo`
      * @return The newly created `Foo`
      */
    def createFoo(name: String): ZIO[Any, Nothing, Foo] =
      fooRepository.create(name)

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
        fooBar      <- findFoos(fooId, otherId)
        mergeResult <- doMergeFoos(fooBar)
      } yield mergeResult

    private def findFoos(fooId: Int, otherId: Int) = {
      val foo = fooRepository.fetch(fooId)
      val bar = fooRepository.fetch(otherId)
      foo.zip(bar).map { case (f, b) => (f, b).tupled }
    }

    private def doMergeFoos(fooPairOpt: Option[(Foo, Foo)])
      : ZIO[Any, Nothing, Option[(Foo, Foo)]] = {
      ZIO
        .sequence(
          for {
            fooPair    <- fooPairOpt
            (foo, bar) = fooPair
          } yield
            for {
              fooOpt <- fooRepository.update(foo.id, foo.name + " " + bar.name)
              barOpt <- fooRepository.update(bar.id, bar.name + " " + foo.name)
            } yield (fooOpt, barOpt).tupled)
        .map(_.headOption.flatten)
    }
  }
}
