package app

import domain._
import zio.ZIO
import zio.console._

import scala.language.higherKinds

object Program {

  val BAD_ID = 42069
  val FAILURE_COMMENT =
    "// failure to merge means nothing is merged so a \"None\" is expected"
  val SUCCESS_COMMENT =
    "// displays foos with IDs 1 and 2, along with their merged names \"foo bar\" and \"bar foo\""

  trait Env extends FooService with Console

  type Result = (Option[(Foo, Foo)], Option[(Foo, Foo)])

  /**
    * This is the whole program. It test-runs a failure and a success case.
    *
    * @return
    */
  def apply(): ZIO[Env, Nothing, Int] = {
    for {
      fooService <- ZIO.access[Env](_.fooService)
      foo        <- fooService.createFoo("foo")
      bar        <- fooService.createFoo(name = "bar")
      failure    <- fooService.mergeFoos(BAD_ID, bar.id)
      success    <- fooService.mergeFoos(foo.id, bar.id)
      _          <- putStrLn(FAILURE_COMMENT)
      failureMsg = s"Failing result: ${failure.toString}"
      _          <- putStrLn(failureMsg)
      _          <- putStrLn(SUCCESS_COMMENT)
      successMsg = s"Successful result: ${success.toString}"
      _          <- putStrLn(successMsg)
    } yield 0
  }
}
