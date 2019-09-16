package domain

import scala.language.higherKinds

trait Transactor[F[_]] {

  val transactor: Transactor.Service[F]
}

object Transactor {

  trait Service[F[_]] {

    def transact[A](fa: F[A]): F[A]
  }

  final case class InMemoryTransactor[F[_]]() extends Service[F] {

    override def transact[A](fa: F[A]): F[A] = fa
  }
}
