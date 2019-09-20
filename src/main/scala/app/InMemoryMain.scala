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
      map          <- Ref.make(Map.empty[String, Foo])
      counter      <- Ref.make(0L)
      programUIO   = Program.program[AppEnvironment, UIO, Any, Nothing]
      environment  = createEnvironment(map, counter)
      result       <- programUIO.provideSome(environment)
      (res1, res2) = result
      _            <- env.console.putStrLn(s"Failing result: ${res1.toString}")
      exitCode <- env.console
                   .putStrLn(s"Successful result: ${res2.toString}")
                   .fold(_ => 1, _ => 0)
    } yield exitCode

  /**
    * Creates the whole object graph needed for the program to run.
    */
  private def createEnvironment(map: Ref[Map[String, Foo]],
                                counter: Ref[Long]) = { base: Environment =>
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
