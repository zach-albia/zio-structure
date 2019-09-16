package domain

import zio.ZIO

import scala.language.higherKinds

object FooService {

  type Environment[F[_], S, E] = FooRepository[F] with HasFunctionK[F, ZIO[S, E, *]]

  trait Service [R <: Environment[F, S, E], F[_], S, E] {
    type EnvBound = Environment[F, S, E]

    def createFoo(name: String): ZIO[S with EnvBound, E, Foo] = for {
      env <- ZIO.environment[EnvBound]
      foo <- env.functionK(env.fooRepository.create(name))
    } yield foo
  }
}
