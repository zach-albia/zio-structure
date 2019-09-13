package domain

import zio.ZIO

import scala.language.higherKinds

object FooService {

  type Environment[S, E] = FooRepository[ZIO[S, E, *]]

  trait Service [R <: Environment[S, E], S, E] {

    def createFoo(name: String): ZIO[S with Environment[S, E], E, Foo] = for {
      env <- ZIO.environment[Environment[S, E]]
      foo <- env.fooRepository.create(name)
    } yield foo
  }
}
