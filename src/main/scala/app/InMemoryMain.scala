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

  def run(args: List[String]): ZIO[InMemoryMain.Environment, Nothing, Int] =
    for {
      env     <- ZIO.environment[InMemoryMain.Environment]
      map     <- Ref.make(Map.empty[String, Foo])
      counter <- Ref.make(0L)
      foos    <- program.provideSome(createEnvironment(map, counter))
      _       <- env.console.putStrLn(s"Failing result: ${foos._1.toString}")
      exitCode <- env.console
                   .putStrLn(s"Successful result: ${foos._2.toString}")
                   .fold(_ => 1, _ => 0)

    } yield exitCode

  /**
    * This is the whole program. It test-runs a failure and a success case.
    *
    * @return
    */
  private def program = {
    val fooService: FooService.Service[AppEnvironment, UIO, Any, Nothing] =
      new FooService.Service[AppEnvironment, UIO, Any, Nothing] {}
    for {
      foo     <- fooService.createFoo("foo")
      bar     <- fooService.createFoo(name = "bar")
      failure <- fooService.mergeFoos("bogus ID", bar.id)
      success <- fooService.mergeFoos(foo.id, bar.id)
    } yield (failure, success)
  }

  /**
    * Creates the whole object graph needed for the program to run.
    */
  private def createEnvironment(map: Ref[Map[String, Foo]],
                                counter: Ref[Long]) = {
    base: InMemoryMain.Environment =>
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
