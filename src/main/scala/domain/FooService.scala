package domain

import cats.implicits._
import com.vladkopanev.zio.saga.Saga._
import zio.{IO, ZIO, ZLayer}

import scala.language.{higherKinds, implicitConversions}

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
    def createFoo(name: String): IO[SaveError, Foo] =
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
    def mergeFoos(fooId: Int, otherId: Int): IO[SaveError, Option[(Foo, Foo)]] =
      for {
        fooBar      <- findFoos(fooId, otherId)
        mergeResult <- doMergeFoos(fooBar)
      } yield mergeResult

    private def findFoos(fooId: Int, otherId: Int) = {
      val foo = fooRepository.fetch(fooId)
      val bar = fooRepository.fetch(otherId)
      foo.zip(bar).map(_.tupled)
    }

    private def doMergeFoos(fooPairOpt: Option[(Foo, Foo)]) = {
      val optZio = (for {
        fooPair              <- fooPairOpt
        (foo, bar)           = fooPair
        (fooUpdate, fooUndo) = updateAndUndo(foo, bar)
        (barUpdate, barUndo) = updateAndUndo(bar, foo)
      } yield
        for {
          fooOpt <- fooUpdate.compensate(fooUndo)
          barOpt <- barUpdate.compensate(barUndo)
        } yield (fooOpt, barOpt).tupled).map(_.transact)
      ZIO.collectAll(optZio).map(_.headOption.flatten)
    }

    private def updateAndUndo(foo: Foo, other: Foo) = {
      val addition  = " " + other.name
      val fooUpdate = fooRepository.update(foo.id, foo.name + addition)
      val fooUndo = fooRepository
        .update(foo.id, foo.name.replace(addition, ""))
        .unit
      (fooUpdate, fooUndo)
    }
  }

  val any: ZLayer[FooService, Nothing, FooService] =
    ZLayer.requires[FooService]

  val live: ZLayer[FooRepository, Nothing, FooService] =
    ZLayer.fromService(repo => new Service { val fooRepository = repo })

  def createFoo(name: String): ZIO[FooService, SaveError, Foo] =
    ZIO.accessM(_.get.createFoo(name))

  def mergeFoos(fooId: Int,
                otherId: Int): ZIO[FooService, SaveError, Option[(Foo, Foo)]] =
    ZIO.accessM(_.get.mergeFoos(fooId, otherId))
}
