package app

import cats.arrow.FunctionK
import com.rms.miu.slickcats.DBIOInstances._
import domain.{FooRepository, HasFunctionK, Transactor}
import persistence._
import slick.jdbc.H2Profile
import slick.jdbc.H2Profile.api._
import zio._
import zio.console._

import scala.concurrent.ExecutionContext.Implicits.global

object SlickMain extends App {

  trait AppEnvironment
      extends FooRepository[DBIO]
      with Console
      with HasFunctionK[DBIO, SlickZIO]
      with Transactor[DBIO]
      with SlickDatabase

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    (for {
      _            <- ZIO.environment[Environment]
      h2db         = Database.forConfig("h2mem1")
      appEnv       = createAppEnv(h2db)
      program      = Program[AppEnvironment, DBIO, SlickDatabase, Throwable]
      _            <- SlickZIO(Foos.foos.schema.create).provideSome(appEnv)
      result       <- program.provideSome(appEnv)
      (res1, res2) = result
      failureMsg   = s"Failing result: ${res1.toString}"
      _            <- console.putStrLn(failureMsg)
      successMsg   = s"Successful result: ${res2.toString}"
      exitCode     <- console.putStrLn(successMsg)
    } yield exitCode).foldM(printError, _ => ZIO.succeed(0))
  }

  private def createAppEnv(db: H2Profile.backend.Database) = {
    base: Environment =>
      new AppEnvironment {
        val transactor: Transactor.Service[DBIO] =
          SlickTransactor
        val console: Console.Service[Any]        = base.console
        val functionK: FunctionK[DBIO, SlickZIO] = SlickFunctionK
        val database: H2Profile.backend.Database = db
        val fooRepository: FooRepository.Service[DBIO] =
          FooRepositorySlick()
      }
  }

  private def printError(err: Throwable) =
    putStrLn(
      s"Execution failed with: $err\nStack " +
        s"trace:\n${err.getStackTrace
          .map(_.toString)
          .mkString("\n")}") *> ZIO.succeed(1)
}
