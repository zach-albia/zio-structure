//package persistence.slick_
//
//import domain._
//import slick.jdbc.H2Profile
//import slick.jdbc.H2Profile.api._
//import zio.ZIO
//
//import scala.language.implicitConversions
//
//object SlickFooService extends FooService.Service {
//
//  val IGNORED_PLACEHOLDER = 42069
//
//  def transactor[R <: DBIO[A], A](program: ZIO[R, Nothing, A]): ZIO[R, Nothing, A] =
//    program.provideSome[DBIO[A]](_.transactionally)
//
//  val fooRepository: FooRepository.Service = ???
//}
