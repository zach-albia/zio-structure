package app

import domain.{Foo, FooRepository, FooService}
import zio.console.Console
import zio.{App, Ref, ZIO}

object Main extends App {

  trait AppEnvironment extends FooRepository[ZIO[Any, Nothing, *]] with Console
  val fooService = new FooService.Service[AppEnvironment, Any, Nothing] {}

  def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] = for {
    map <- Ref.make(Map.empty[String, Foo])
    counter <- Ref.make(0L)
    result <- fooService.createFoo("bar").provideSome[Environment]{ base =>
      new AppEnvironment {
        override val console: Console.Service[Any] = base.console
        override val fooRepository = FooRepository.InMemoryFooRepository(map, counter)
      }
    }
    env <-ZIO.environment[Main.Environment]
    _ <- env.console.putStr(result.toString)
  } yield 1
}
