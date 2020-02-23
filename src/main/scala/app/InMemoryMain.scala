package app

import domain._
import zio._
import zio.console._

object InMemoryMain extends App {

  val env = Console.live ++ (FooRepository.inMem >>> FooService.live)

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    Program().provideLayer(env).fold(_ => 1, _ => 0)
}
