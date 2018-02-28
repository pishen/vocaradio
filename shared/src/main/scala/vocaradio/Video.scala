package vocaradio

case class Snippet(title: String)
case class ContentDetails(duration: String)
case class Video(
  id: String,
  snippet: Snippet,
  contentDetails: ContentDetails
)
