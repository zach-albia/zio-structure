package app

import cats.arrow.FunctionK
import domain.{Foo, FooRepository, FooService, HasFunctionK}
import zio.console.Console
import zio.{App, Ref, UIO, ZIO}

object Main extends App {

  trait AppEnvironment
      extends FooRepository[UIO]
      with HasFunctionK[UIO, UIO]
      with Console

  val fooService: FooService.Service[AppEnvironment, UIO, Any, Nothing] =
    new FooService.Service[AppEnvironment, UIO, Any, Nothing] {}

  def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] =
    for {
      env <- ZIO.environment[Main.Environment]
      map <- Ref.make(Map.empty[String, Foo])
      counter <- Ref.make(0L)
      result <- fooService.createFoo("bar").provideSome[Environment] { base =>
        new AppEnvironment {
          override val console: Console.Service[Any] = base.console
          override val functionK: FunctionK[UIO, UIO] =
            new FunctionK[UIO, UIO] {
              override def apply[A](fa: UIO[A]): UIO[A] = fa
            }
          override val fooRepository =
            FooRepository.InMemoryFooRepository(map, counter)
        }
      }
      exitCode <- env.console
        .putStrLn(result.toString)
        .fold(_ => 1, _ => 0)

    } yield exitCode
}
