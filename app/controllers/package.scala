import akka.actor.actorRef2Scala
import controllers.Application
import play.api.libs.json.JsValue
import play.api.libs.json.Json

package object controllers {
  def currentTime() = System.currentTimeMillis() / 1000
  def broadcast(json: JsValue) = Application.broadcaster ! ToAll(Json.stringify(json))
}