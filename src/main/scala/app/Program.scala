package app

import cats.Monad
import cats.arrow.FunctionK
import domain._
import zio.{UIO, ZIO}

import scala.language.higherKinds

object Program {

  /** These are all this program's environment members in one trait */
  trait Environment[F[_]]
      extends FooRepository[F]
      with HasFunctionK[F, ZIO[Any, Nothing, *]]
      with Transactor[F]

  type Result = (Option[(Foo, Foo)], Option[(Foo, Foo)])

  /**
    * This is the whole program. It test-runs a failure and a success case.
    *
    * @return
    */
  def apply[F[_]]()(
      implicit F: Monad[F]): ZIO[Environment[F], Nothing, Result] = {
    for {
      env <- ZIO.environment[Environment[F]]
      fooService = new FooService.Service[F] {
        val fooRepository = env.fooRepository
        val toZIO         = env.functionK
        val transact      = env.transact
      }
      foo     <- fooService.createFoo("foo")
      bar     <- fooService.createFoo(name = "bar")
      failure <- fooService.mergeFoos(42069, bar.id)
      success <- fooService.mergeFoos(foo.id, bar.id)
    } yield (failure, success)
  }
}
