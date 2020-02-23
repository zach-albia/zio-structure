// from: https://gist.github.com/strobe/6c8c860e442318ad0577cea4255e0507
// val ZIOVersion = "1.0.0-RC17+430-2e51f657-SNAPSHOT"
// resolvers += Resolver.sonatypeRepo("snapshots")
//
// libraryDependencies ++= Seq(
//   // zio
//   "dev.zio" %% "zio"         % ZIOVersion,
//   "dev.zio" %% "zio-streams" % ZIOVersion,
// )

import zio._
import zio.console._
import zio.clock._
import java.io.IOException
import zio.duration.Duration._

package object moduleA {
  type ModuleA = Has[ModuleA.Service]

  object ModuleA {
    trait Service {
      def letsGoA(v: Int): UIO[String]
    }

    val any: ZLayer[ModuleA, Nothing, ModuleA] =
      ZLayer.requires[ModuleA]

    val live: ZLayer.NoDeps[Nothing, ModuleA] = ZLayer.succeed { v: Int =>
      UIO(s"done: v = $v ")
    }
  }

  def letsGoA(v: Int): ZIO[ModuleA, Nothing, String] =
    ZIO.accessM(_.get.letsGoA(v))
}

import moduleA._

package object moduleB {
  type ModuleB = Has[ModuleB.Service]

  object ModuleB {
    trait Service {
      def letsGoB(v: Int): UIO[String]
    }

    val any: ZLayer[ModuleB, Nothing, ModuleB] =
      ZLayer.requires[ModuleB]

    val live: ZLayer[ModuleA, Nothing, ModuleB] = ZLayer.fromService {
      moduleA => (v: Int) =>
        moduleA.letsGoA(v)
    }
  }

  def letsGoB(v: Int): ZIO[ModuleB, Nothing, String] =
    ZIO.accessM(_.get.letsGoB(v))
}

object ZLayersApp extends zio.App {

  import moduleB._

  val env = Console.live ++ Clock.live ++ (ModuleA.live >>> ModuleB.live)
  val program: ZIO[Console with Clock with ModuleB, IOException, Unit] =
    for {
      _ <- putStrLn(s"Welcome to ZIO!")
      _ <- sleep(Finite(1000))
      r <- letsGoB(10)
      _ <- putStrLn(r)
    } yield ()

  def run(args: List[String]) =
    program.provideLayer(env).fold(_ => 1, _ => 0)

}

// output:
// [info] running ZLayersApp
// Welcome to ZIO!
// done: v = 10
