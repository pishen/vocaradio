import scala.concurrent.duration.DurationInt
import akka.util.Timeout
import play.api.libs.json.JsValue
import play.api.libs.json.Json

package object controllers {
  implicit val timeout = Timeout((5).seconds)
  
  def currentTime = System.currentTimeMillis() / 1000
  def broadcast(json: JsValue) = Application.broadcaster ! ToAll(Json.stringify(json))
}