package app

import domain._
import zio.{Has, ZIO}
import zio.console._

import scala.language.higherKinds

object Program {

  val BAD_ID = 42069
  val FAILURE_COMMENT =
    "// failure to merge means nothing is merged so a \"None\" is expected"
  val SUCCESS_COMMENT =
    "// displays foos with IDs 1 and 2, along with their merged names \"foo bar\" and \"bar foo\""

  type Env = Has[FooService] with Has[Console]

  type Result = (Option[(Foo, Foo)], Option[(Foo, Foo)])

  import domain.FooService._

  /**
    * This is the whole program. It test-runs a failure and a success case.
    *
    * @return
    */
  def apply(): ZIO[Console with FooService, SaveError, Unit] = {
    for {
      foo        <- createFoo("foo")
      bar        <- createFoo("bar")
      failure    <- mergeFoos(BAD_ID, bar.id)
      success    <- mergeFoos(foo.id, bar.id)
      _          <- putStrLn(FAILURE_COMMENT)
      failureMsg = s"Failing result: ${failure.toString}"
      _          <- putStrLn(failureMsg)
      _          <- putStrLn(SUCCESS_COMMENT)
      successMsg = s"Successful result: ${success.toString}"
      _          <- putStrLn(successMsg)
    } yield ()
  }

  def printError(err: SaveError): ZIO[Console, Nothing, Int] =
    putStrLn(err.message) *> ZIO.succeed(1)
}
