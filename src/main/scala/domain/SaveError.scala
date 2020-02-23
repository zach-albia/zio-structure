package domain

sealed trait SaveError {
  def message: String
}

object SaveError {
  final case class NameAlreadyExists(name: String) extends SaveError {
    def message = s"Name '${name}' already exists."
  }
  final case class ThrowableError(e: Throwable) extends SaveError {
    def message = e.getMessage
  }
}
