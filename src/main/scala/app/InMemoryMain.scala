package app

import domain._
import zio._

object InMemoryMain extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    for {
      map        <- Ref.make(Map.empty[Int, Foo])
      counter    <- Ref.make(0)
      programUIO = Program()
      appEnv     = createAppEnv(map, counter)
      exitCode   <- programUIO.provideSome(appEnv)
    } yield exitCode

  /**
    * Creates the whole object graph needed for the program to run.
    */
  private def createAppEnv(map: Ref[Map[Int, Foo]], counter: Ref[Int]) = {
    base: ZEnv =>
      new Program.Env {
        val console = base.console
        val fooService = new FooService.Service {
          val fooRepository = FooRepository.InMemoryFooRepository(map, counter)
        }
      }
  }
}
