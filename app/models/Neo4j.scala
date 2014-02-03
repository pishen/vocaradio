package models

import scala.collection.JavaConversions.iterableAsScalaIterable
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import akka.actor.Actor
import akka.actor.actorRef2Scala
import play.api.Logger

class Neo4j extends Actor {
  Logger.info("start graph-db")
  val graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("graph-db")

  createIndexIfNotExist(Labels.song, "originTitle")
  createIndexIfNotExist(Labels.song, "videoId")

  def receive = {
    case CreateNode(label, kv) => withTx {
      val node = graphDb.createNode(label)
      kv.foreach { case (key, value) => node.setProperty(key, value) }
      sender ! CreateNodeResponse(node.getId())
    }
    case GetNodes(label, key, value) => withTx {
      val res = graphDb.findNodesByLabelAndProperty(label, key, value).map(_.getId()).toSeq
      sender ! GetNodesResponse(res)
    }
    case GetProperties(ids, keys) => withTx {
      val res = ids.map(id => {
        val node = graphDb.getNodeById(id)
        val kv = keys
          .map(key => (key, node.getProperty(key, null).asInstanceOf[String]))
          .filter(_._2 != null).toMap
        PartialNode(id, kv)
      })
      sender ! GetPropertiesResponse(res)
    }
    case SetProperties(id, kv) => withTx {
      val node = graphDb.getNodeById(id)
      kv.foreach { case (key, value) => node.setProperty(key, value) }
    }
  }

  def createIndexIfNotExist(label: Label, key: String) = withTx {
    if (!graphDb.schema().getIndexes(label).exists(_.getPropertyKeys().exists(_ == key))) {
      graphDb.schema().indexFor(label).on(key).create()
    }
  }

  def withTx(operations: => Unit) = {
    val tx = graphDb.beginTx()
    try {
      operations
      tx.success()
    } finally {
      tx.close()
    }
  }

  override def postStop() = {
    Logger.info("shutdown graph-db")
    graphDb.shutdown();
  }
}

object Labels {
  val song = DynamicLabel.label("Song")
}

//requests (kv: key-value pairs)
case class CreateNode(label: Label, kv: Seq[(String, String)])
case class GetNodes(label: Label, key: String, value: String)
case class GetProperties(ids: Seq[Long], keys: Seq[String])
case class SetProperties(id: Long, kv: Seq[(String, String)])

//responses
case class CreateNodeResponse(id: Long)
case class GetNodesResponse(ids: Seq[Long])
case class GetPropertiesResponse(nodes: Seq[PartialNode])

//extra structures
case class PartialNode(id: Long, kv: Map[String, String])