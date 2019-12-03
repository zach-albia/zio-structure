package app

import cats.arrow.FunctionK
import domain._
import zio._
import zio.console._
import zio.interop.catz._

object InMemoryMain extends App {

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    for {
      map        <- Ref.make(Map.empty[Int, Foo])
      counter    <- Ref.make(0)
      programUIO = Program[UIO]
      appEnv     = createAppEnv(map, counter)
      exitCode   <- programUIO.provideSome(appEnv)
    } yield exitCode

  /**
    * Creates the whole object graph needed for the program to run.
    */
  private def createAppEnv(map: Ref[Map[Int, Foo]], counter: Ref[Int]) = {
    base: Environment =>
      new Program.Environment[UIO] {
        val console = base.console
        val fooService = new FooService.Service[UIO] {
          val transact      = Transactor.InMemoryTransactor()
          val toZIO         = FunctionK.id[UIO]
          val fooRepository = FooRepository.InMemoryFooRepository(map, counter)
        }
      }
  }
}
