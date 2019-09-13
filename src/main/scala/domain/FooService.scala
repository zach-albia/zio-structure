package domain

import zio.ZIO

import scala.language.higherKinds

object FooService {

  type Environment[F[_], S, E] = FooRepository[F] with HasFunctionK[F, ZIO[S, E, *]]

  trait Service [R <: Environment[F, S, E], F[_], S, E] {

    def createFoo(name: String): ZIO[S with Environment[F, S, E], E, Foo] = for {
      env <- ZIO.environment[Environment[F, S, E]]
      foo <- env.functionK(env.fooRepository.create(name))
    } yield foo
  }
}
