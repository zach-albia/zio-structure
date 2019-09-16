package app

import cats.arrow.FunctionK
import domain._
import zio.console.Console
import zio.interop.catz._
import zio.{App, Ref, UIO, ZIO}

object Main extends App {

  trait AppEnvironment
      extends FooRepository[UIO]
      with Console
      with HasFunctionK[UIO, UIO]
      with Transactor[UIO]

  val fooService: FooService.Service[AppEnvironment, UIO, Any, Nothing] =
    new FooService.Service[AppEnvironment, UIO, Any, Nothing] {}

  def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] =
    for {
      env <- ZIO.environment[Main.Environment]
      map <- Ref.make(Map.empty[String, Foo])
      counter <- Ref.make(0L)
      foos <- (for {
        foo <- fooService.createFoo("foo")
        bar <- fooService.createFoo(name = "bar")
        failure <- fooService.mergeFoos("bogus ID", bar.id)
        success <- fooService.mergeFoos(foo.id, bar.id)
      } yield (failure, success)).provideSome[Environment] { base =>
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
      _ <- env.console.putStrLn(s"Failing result: ${foos._1.toString}")
      exitCode <- env.console
        .putStrLn(s"Successful result: ${foos._2.toString}")
        .fold(_ => 1, _ => 0)

    } yield exitCode
}
