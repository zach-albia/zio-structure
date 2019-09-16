package domain

import cats.Monad
import cats.implicits._
import zio.ZIO

import scala.language.higherKinds

object FooService {

  type Environment[F[_], S, E] = FooRepository[F]
    with HasFunctionK[F, ZIO[S, E, *]]
    with Transactor[F]

  trait Service[R <: Environment[F, S, E], F[_], S, E] {

    type EnvBound = Environment[F, S, E]

    def createFoo(name: String): ZIO[S with EnvBound, E, Foo] =
      for {
        env <- ZIO.environment[EnvBound]
        foo <- env.functionK(env.fooRepository.create(name))
      } yield foo

    def mergeFoos(fooId: String, otherId: String)(implicit MonadF: Monad[F])
      : ZIO[S with EnvBound, E, Option[(Foo, Foo)]] =
      for {
        env <- ZIO.environment[EnvBound]
        foosOpt <- ZIO
          .sequence(
            List(
              env.functionK(env.fooRepository.fetch(fooId)),
              env.functionK(env.fooRepository.fetch(otherId))
            ))
          .map(_.sequence)
        resOpt = for {
          foos <- foosOpt
          fooPair <- foos.headOption.zip(foos.tail.headOption)
          fa = for {
            foo <- env.fooRepository
              .update(fooId, fooPair._1.name + " " + fooPair._2.name)
            other <- env.fooRepository
              .update(fooId, fooPair._2.name + " " + fooPair._1.name)
          } yield List(foo, other)
          res = env.functionK(env.transactor.transact(fa))
        } yield res
        result <- ZIO.sequence(resOpt)
        mergedFoos = result.flatten.sequence
          .flatMap(l => l.headOption.zip(l.tail.headOption))
      } yield mergedFoos
  }
}
