package app

import cats.Monad
import domain.{Foo, FooService}
import domain.FooService.Environment
import zio.ZIO

import scala.language.higherKinds

object Program {

  type Result = (Option[(Foo, Foo)], Option[(Foo, Foo)])

  /**
    * This is the whole program. It test-runs a failure and a success case.
    *
    * @return
    */
  def apply[R <: FooService.Environment[F, S, E], F[_], S, E]()(
      implicit F: Monad[F]): ZIO[S with Environment[F, S, E], E, Result] = {
    val fooService = new FooService.Service[R, F, S, E] {}
    for {
      foo     <- fooService.createFoo("foo")
      bar     <- fooService.createFoo(name = "bar")
      failure <- fooService.mergeFoos(42069, bar.id)
      success <- fooService.mergeFoos(foo.id, bar.id)
    } yield (failure, success)
  }
}
