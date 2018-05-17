package vocaradio

case class Song(
  query: String,
  idOpt: Option[String],
  active: Boolean
)
