package app

import domain._
import zio._
import zio.console._

object InMemoryMain extends App {

  trait Env extends Program.Env with Console

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    for {
      map                <- Ref.make(Map.empty[Int, Foo])
      counter            <- Ref.make(0)
      programUIO         = Program()
      appEnv             = createEnv(map, counter)
      result             <- programUIO.provideSome(appEnv)
      (failure, success) = result
      failureMsg = s"Failing result: ${failure.toString}\n// failure to " +
        "merge means nothing is merged so a \"None\" is expected"
      _ <- putStrLn(failureMsg)
      successMsg = s"Successful result: ${success.toString} \n// displays " +
        "foos with IDs 1 and 2, along with their merged names \"foo bar\"" +
        " and \"bar foo\""
      _ <- putStrLn(successMsg)
    } yield 0

  /**
    * Creates the whole object graph needed for the program to run.
    */
  private def createEnv(map: Ref[Map[Int, Foo]],
                        counter: Ref[Int]): ZEnv => Env = { base =>
    new Env {
      val fooService = FooService.InMemoryFooService(
        FooRepository.InMemoryFooRepository(map, counter))
      val console = base.console
    }
  }
}
