package lichess

case class LightUser(
  id: String,
  name: String,
  title: Option[String] = None)

case class Users(white: LightUser, black: LightUser) {

  def apply(color: chess.Color) = color.fold(white, black)
}
