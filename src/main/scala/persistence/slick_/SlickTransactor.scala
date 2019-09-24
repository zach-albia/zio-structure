package persistence.slick_

import domain.Transactor
import slick.jdbc.H2Profile.api._

object SlickTransactor extends Transactor.Service[DBIO] {

  override def apply[A](fa: DBIO[A]): DBIO[A] = fa.transactionally
}
