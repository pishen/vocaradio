package vocaradio

import io.circe._

object CirceHelpers {
  implicit class RichJson(json: Json) {
    def \(key: String) = json.hcursor.downField(key)
  }

  implicit class RichACursor(cursor: ACursor) {
    def \(key: String) = cursor.downField(key)
    def asUnsafe[T: Decoder] = cursor.as[T].toTry.get
  }
}
