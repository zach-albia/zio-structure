package persistence

import domain.Foo
import slick.jdbc.H2Profile.api._

class Foos(tag: Tag) extends Table[Foo](tag, "FOOS") {
  def id   = column[Int]("ID", O.PrimaryKey)
  def name = column[String]("NAME", O.Unique, O.Length(120, varying = true))
  def *    = (id, name) <> (Foo.tupled, Foo.unapply)
}

object Foos {
  val foos = TableQuery[Foos]
}
