package app

import com.rms.miu.slickcats.DBIOInstances._
import persistence.slick_._
import slick.jdbc.H2Profile
import slick.jdbc.H2Profile.api._
import zio._
import zio.console._

import scala.concurrent.ExecutionContext.Implicits.global

object SlickMain extends App {

  def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    (for {
      _                  <- ZIO.environment[Environment]
      h2db               = Database.forConfig("h2mem1")
      env                = createAppEnv(h2db)
      program            = Program[DBIO]
      result             <- (SlickZIO(Foos.foos.schema.create) *> program).provideSome(env)
      (failure, success) = result
      _ <- putStrLn(
            "// failure to merge means nothing is merged so a \"None\" is expected")
      failureMsg = s"Failing result: ${failure.toString}"
      _          <- putStrLn(failureMsg)
      _ <- putStrLn(
            "// displays foos with IDs 1 and 2, along with their merged names \"foo bar\" and \"bar foo\"")
      successMsg = s"Successful result: ${success.toString}"
      exitCode   <- putStrLn(successMsg)
    } yield exitCode).foldM(printError, _ => ZIO.succeed(0))
  }

  private def createAppEnv(h2db: H2Profile.backend.Database)
    : Environment => Program.Environment[DBIO] with SlickDatabase = { _ =>
    new Program.Environment[DBIO] with SlickDatabase {
      val database      = h2db
      val transact      = SlickTransactor
      val functionK     = new SlickFunctionK { val db = h2db }
      val fooRepository = SlickFooRepository()
    }
  }

  private def printError(err: Throwable) =
    putStrLn(
      s"Execution failed with: $err\nStack " +
        s"trace:\n${err.getStackTrace
          .map(_.toString)
          .mkString("\n")}") *> ZIO.succeed(1)
}
