package app

import domain.FooService
import domain.SaveError.ThrowableError
import persistence.slick_.Foos.foos
import persistence.slick_._
import slick.jdbc.H2Profile.api._
import zio._
import zio.console._

import scala.concurrent.ExecutionContext.Implicits.global

object SlickMain extends App {

  val slickDB: ZLayer.NoDeps[Nothing, SlickDatabase] =
    SlickDatabase.live("h2mem1")

  val env: ZLayer[Any, Nothing, Console with SlickDatabase with FooService] =
    Console.live ++ slickDB ++ (((slickDB ++ ec(global)) >>>
      SlickFooRepository.live) >>> FooService.live)

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    (for {
      _ <- SlickDatabase.run(foos.schema.create).mapError(ThrowableError)
      _ <- Program()
    } yield ())
      .provideLayer(env)
      .foldM(Program.printError, _ => ZIO.succeed(0))
}
