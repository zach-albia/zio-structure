package app

import persistence.slick_.Foos.foos
import persistence.slick_._
import slick.jdbc.H2Profile.api._
import zio._
import zio.console._

import scala.concurrent.ExecutionContext.Implicits.global

object SlickMain extends App {

  trait Env extends Program.Env with SlickDatabase with Console

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val appEnv = createEnv(Database.forConfig("h2mem1"))
    (for {
      _                  <- SlickZIO(foos.schema.create)
      result             <- Program()
      (failure, success) = result
      failureMsg = s"Failing result: ${failure.toString}\n// failure to " +
        "merge means nothing is merged so a \"None\" is expected"
      _ <- putStrLn(failureMsg)
      successMsg = s"Successful result: ${success.toString} \n// " +
        "successfully displays foos with IDs 1 and 2, along with their " +
        "merged names \"foo bar\" and \"bar foo\""
      exitCode <- putStrLn(successMsg)
    } yield exitCode)
      .provideSome(appEnv)
      .foldM(printError _, _ => ZIO.succeed(0))
  }

  private def createEnv(h2db: Database): ZEnv => Env = { base =>
    new Env {
      val fooService = createFooService(h2db)
      val database   = h2db
      val console    = base.console
    }
  }

  private def createFooService(h2db: Database) =
    new SlickFooService.Service {
      lazy val db   = h2db
      lazy val ec   = global
    }

  private def printError(err: Throwable) =
    putStrLn(
      s"Execution failed with: $err\nStack " +
        s"trace:\n${err.getStackTrace
          .map(_.toString)
          .mkString("\n")}") *> ZIO.succeed(1)
}
