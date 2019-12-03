package app

import com.rms.miu.slickcats.DBIOInstances._
import domain.FooService
import persistence.slick_._
import slick.jdbc.H2Profile
import slick.jdbc.H2Profile.api._
import zio._
import zio.console._

import scala.concurrent.ExecutionContext.Implicits.global

object SlickMain extends App {

  def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    (for {
      _       <- ZIO.environment[Environment]
      h2db    = Database.forConfig("h2mem1")
      env     = createAppEnv(h2db)
      program = Program[DBIO]
      exitCode <- (SlickZIO(Foos.foos.schema.create) *> program)
                   .provideSome(env)
    } yield exitCode).foldM(printError, _ => ZIO.succeed(0))
  }

  private def createAppEnv(h2db: H2Profile.backend.Database)
    : Environment => Program.Environment[DBIO] with SlickDatabase = { base =>
    new Program.Environment[DBIO] with SlickDatabase {
      val console  = base.console
      val database = h2db
      val fooService = new FooService.Service[DBIO] {
        val transact      = SlickTransactor
        val fooRepository = SlickFooRepository()
        val toZIO         = new SlickFunctionK { override val db = h2db }
      }
    }
  }

  private def printError(err: Throwable) =
    putStrLn(
      s"Execution failed with: $err\nStack " +
        s"trace:\n${err.getStackTrace
          .map(_.toString)
          .mkString("\n")}") *> ZIO.succeed(1)
}
