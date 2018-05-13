package vocaradio

case class Thumbnail(url: String)
case class Thumbnails(medium: Thumbnail)
case class Snippet(
  title: String,
  thumbnails: Thumbnails
)
case class RegionRestriction(blocked: Option[List[String]])
case class ContentDetails(
  duration: String,
  regionRestriction: Option[RegionRestriction]
)
case class Video(
  id: String,
  snippet: Snippet,
  contentDetails: ContentDetails
)
