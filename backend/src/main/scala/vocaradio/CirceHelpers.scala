package vocaradio

import scala.concurrent.Future

import io.circe._
import Global._

object CirceHelpers {
  implicit class RichJson(json: Json) {
    def \(key: String) = json.hcursor.downField(key)
    def asF[T: Decoder] = Future(json.as[T].toTry.get)
  }
  implicit class RichACursor(cursor: ACursor) {
    def \(key: String) = cursor.downField(key)
    def asF[T: Decoder] = Future(cursor.as[T].toTry.get)
  }
}
