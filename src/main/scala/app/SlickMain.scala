package app

import domain.FooService
import persistence.slick_.Foos.foos
import persistence.slick_._
import slick.jdbc.H2Profile
import slick.jdbc.H2Profile.api._
import zio._

import scala.concurrent.ExecutionContext.Implicits.global

object SlickMain extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    (SlickZIO(foos.schema.create) *> Program())
      .provideSome(createAppEnv(Database.forConfig("h2mem1")))
      .foldM(Program.printError, _ => ZIO.succeed(0))

  private def createAppEnv(h2db: H2Profile.backend.Database)
    : ZEnv => Program.Env with SlickDatabase = { base =>
    new Program.Env with SlickDatabase {
      val console  = base.console
      val database = h2db
      val fooService = new FooService.Service {
        val fooRepository = new SlickFooRepository {
          val slickDatabase = new SlickDatabase {
            override val database = h2db
          }
          implicit val ec = global // let Slick use its own thread pool
        }
      }
    }
  }

}
