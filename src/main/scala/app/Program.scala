package app

import domain._
import zio.ZIO

import scala.language.higherKinds

object Program {

  /** These are all this program's environment members in one trait */
  type Env = FooService

  type Result = (Option[(Foo, Foo)], Option[(Foo, Foo)])

  /**
    * This is the whole program. It test-runs a failure and a success case.
    *
    * @return
    */
  def apply(): ZIO[Env, Nothing, Result] = {
    for {
      fooService <- ZIO.access[Env](_.fooService)
      foo        <- fooService.createFoo("foo")
      bar        <- fooService.createFoo(name = "bar")
      failure    <- fooService.mergeFoos(42069, bar.id)
      success    <- fooService.mergeFoos(foo.id, bar.id)
    } yield (failure, success)
  }
}
