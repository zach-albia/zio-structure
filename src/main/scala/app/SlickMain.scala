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
      _            <- ZIO.environment[Environment]
      h2db         = Database.forConfig("h2mem1")
      appEnv       = createAppEnv(h2db)
      program      = Program[DBIO]
      _            <- SlickZIO(Foos.foos.schema.create).provideSome(appEnv)
      result       <- program.provideSome(appEnv)
      (res1, res2) = result
      failureMsg   = s"Failing result: ${res1.toString}"
      _            <- putStrLn(failureMsg)
      successMsg   = s"Successful result: ${res2.toString}"
      exitCode     <- putStrLn(successMsg)
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
