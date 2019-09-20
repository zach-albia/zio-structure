package app

import cats.arrow.FunctionK
import domain._
import zio._
import zio.console.Console
import zio.interop.catz._

object InMemoryMain extends App {

  /** These are all this program's environment members in one trait */
  trait AppEnvironment
      extends FooRepository[UIO]
      with Console
      with HasFunctionK[UIO, UIO]
      with Transactor[UIO]

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    for {
      env          <- ZIO.environment[Environment]
      map          <- Ref.make(Map.empty[Int, Foo])
      counter      <- Ref.make(0)
      programUIO   = Program[AppEnvironment, UIO, Any, Nothing]
      appEnv       = createAppEnv(map, counter)
      result       <- programUIO.provideSome(appEnv)
      (res1, res2) = result
      failureMsg   = s"Failing result: ${res1.toString}"
      _            <- env.console.putStrLn(failureMsg)
      successMsg   = s"Successful result: ${res2.toString}"
      exitCode     <- env.console.putStrLn(successMsg).fold(_ => 1, _ => 0)
    } yield exitCode

  /**
    * Creates the whole object graph needed for the program to run.
    */
  private def createAppEnv(map: Ref[Map[Int, Foo]], counter: Ref[Int]) = {
    base: Environment =>
      new AppEnvironment {
        override val console: Console.Service[Any] = base.console
        override val functionK: FunctionK[UIO, UIO] =
          new FunctionK[UIO, UIO] {
            override def apply[A](fa: UIO[A]): UIO[A] = fa
          }
        override val fooRepository =
          FooRepository.InMemoryFooRepository(map, counter)
        override val transactor: Transactor.Service[UIO] =
          Transactor.InMemoryTransactor()
      }
  }
}
