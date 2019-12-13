package app

import domain.FooService
import persistence.slick_.Foos.foos
import persistence.slick_._
import slick.jdbc.H2Profile
import slick.jdbc.H2Profile.api._
import zio._
import zio.console._

import scala.concurrent.ExecutionContext

object SlickMain extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    for {
      ec <- ZIO.accessM[ZEnv](_.blocking.blockingExecutor).map(_.asEC)
      result <- (SlickZIO(foos.schema.create) *> Program())
                 .provideSome(createAppEnv(Database.forConfig("h2mem1"), ec))
                 .foldM(printError, _ => ZIO.succeed(0))
    } yield result

  private def createAppEnv(h2db: H2Profile.backend.Database,
                           executionContext: ExecutionContext)
    : ZEnv => Program.Env with SlickDatabase = { base =>
    new Program.Env with SlickDatabase {
      val console  = base.console
      val database = h2db
      val fooService = new FooService.Service {
        val fooRepository = new SlickFooRepository {
          val slickDatabase = new SlickDatabase {
            override val database = h2db
          }
          implicit val ec = executionContext
        }
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
