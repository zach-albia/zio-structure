package domain

import zio.{Ref, UIO}

import scala.language.higherKinds

/**
  * I guess the convention is for traits to have environment members. In this
  * case, `fooRepository` is such a member.
  */
trait FooRepository {

  /** Environment member */
  val fooRepository: FooRepository.Service
}

/**
  * This is the companion object containing the actual interface of this
  * environment member
  */
object FooRepository {

  trait Service {

    def create(name: String): UIO[Foo]

    def fetch(id: Int): UIO[Option[Foo]]

    def update(id: Int, name: String): UIO[Option[Foo]]

    def delete(id: Int): UIO[Unit]
  }

  /**
    * Sample in-memory implementation
    */
  final case class InMemoryFooRepository(
      mapTable: Ref[Map[Int, Foo]],
      idSequence: Ref[Int]
  ) extends Service {

    override def create(name: String): UIO[Foo] =
      for {
        newId <- idSequence.update(_ + 1)
        foo   = Foo(newId, name)
        _     <- mapTable.update(store => store + (newId -> foo))
      } yield foo

    override def fetch(id: Int): UIO[Option[Foo]] =
      mapTable.get.map(_.get(id))

    override def update(id: Int, name: String): UIO[Option[Foo]] =
      for {
        updatedFooOpt <- mapTable.get.map(_.get(id).map(_ => Foo(id, name)))
        _ <- updatedFooOpt
              .map(foo => mapTable.update(store => store + (id -> foo)))
              .getOrElse(UIO.unit)
      } yield updatedFooOpt

    override def delete(id: Int): UIO[Unit] =
      mapTable.update(_ - id).unit
  }

}
