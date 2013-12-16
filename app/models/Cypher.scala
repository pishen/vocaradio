package models

import play.api.libs.ws.WS
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.api.libs.json.Json.toJsFieldJsValueWrapper

class Cypher(json: JsObject) {
  def execute = {
    Cypher.defaultReq.post(json).map(_.json)
  }
  def getSingle = {
    Cypher.defaultReq.post(json).map(resp => (resp.json \ "data")(0)(0).asOpt[String])
  }
  def on(params: (String, String)*) = {
    new Cypher(json.+("params" -> JsObject(params.map { case (k, v) => (k, JsString(v)) })))
  }
}

object Cypher {
  val defaultReq = WS.url("http://localhost:7474/db/data/cypher")
    .withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json")

  def apply(query: String) = {
    new Cypher(Json.obj("query" -> query))
  }

  def main(args: Array[String]): Unit = {
    //REST test
    println("pre-start")
    val f1 = Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} RETURN s.videoId")
      .on("ot" -> "【初音ミク】 君の傷跡 【オリジナル曲】").getSingle
    f1.foreach{
      case Some(vid) => println("vid: " + vid)
      case None => println("no this song")
    }
    println("main finished")
  }
}