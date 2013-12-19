package models

import play.api.libs.ws.WS
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import org.slf4j.LoggerFactory
import Cypher.log

class Cypher(json: JsObject) {
  def execute = {
    val p = Cypher.defaultReq.post(json)
    //p.onFailure { case ex: Exception => log.error("cypher", ex) }
    p.map(_.json)
  }
  def getData = {
    val p = Cypher.defaultReq.post(json)
    //p.onFailure { case ex: Exception => log.error("cypher", ex) }
    p.map(resp => (resp.json \ "data").as[Array[Array[String]]])
  }
  def on(params: (String, String)*) = {
    new Cypher(json.+("params" -> JsObject(params.map { case (k, v) => (k, JsString(v)) })))
  }
}

object Cypher {
  val log = LoggerFactory.getLogger("Cypher")
  val defaultReq = WS.url("http://localhost:7474/db/data/cypher")
    .withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json")

  def apply(query: String) = {
    new Cypher(Json.obj("query" -> query))
  }

  def main(args: Array[String]): Unit = {
    //REST test
    println("pre-start")
    val f1 = Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} RETURN s.videoId")
      .on("ot" -> "【初音ミク】 君の傷跡 【オリジナル曲】").execute
    f1.foreach(println)
    f1.onFailure {
      case ex: Exception => println(ex)
    }
    println("main finished")
  }
}