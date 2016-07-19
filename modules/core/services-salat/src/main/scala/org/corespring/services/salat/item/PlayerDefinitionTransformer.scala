package org.corespring.services.salat.item

import com.mongodb.DBObject
import com.mongodb.casbah.commons.Logger
import com.novus.salat.transformers.CustomTransformer
import org.corespring.models.item.PlayerDefinition
import org.corespring.services.salat.serialization.{ ToJsValue, ToDBObject }
import com.mongodb.casbah.Imports._
import play.api.libs.json.{ Json, JsValue }

case class PlayerDefinitionTransformerException(e: Throwable) extends RuntimeException(e)

/**
 * A transformer to help salat to (de)serialize - jsvalue to and from mongo db
 */
class PlayerDefinitionTransformer(fileTransformer: FileTransformer) extends CustomTransformer[PlayerDefinition, DBObject] {

  lazy val logger = Logger(classOf[PlayerDefinitionTransformer])

  override def serialize(a: PlayerDefinition): DBObject = try {
    logger.trace(s"serialize: ${a}")

    val builder = MongoDBObject.newBuilder

    val preppedFiles: Seq[DBObject] = a.files.map {
      f => fileTransformer.serialize(f)
    }

    builder += "files" -> MongoDBList(preppedFiles: _*)
    builder += "xhtml" -> a.xhtml
    builder += "config" -> ToDBObject(a.config)
    builder += "components" -> ToDBObject(a.components)
    builder += "summaryFeedback" -> a.summaryFeedback
    a.customScoring.foreach { cs =>
      builder += "customScoring" -> cs
    }

    builder.result()
  } catch {
    case e: Throwable => {
      logger.error(e.getMessage)
      throw PlayerDefinitionTransformerException(e)
    }
  }

  override def deserialize(b: DBObject): PlayerDefinition = try {

    import com.mongodb.casbah.Implicits._

    logger.trace(s"deserialize: ${b}")

    val components: JsValue = if (b.get("components") == null) Json.obj() else ToJsValue(b.get("components"))
    val config: JsValue = if (b.get("config") == null) Json.obj() else ToJsValue(b.get("config"))

    val files = if (b.get("files") == null || !b.get("files").isInstanceOf[BasicDBList]) {
      Seq.empty
    } else {
      val list = b.get("files").asInstanceOf[BasicDBList]
      list.toList.map {
        dbo =>
          fileTransformer.deserialize(dbo.asInstanceOf[DBObject])
      }
    }

    val customScoring = if (b.get("customScoring") == null) None else Some(b.get("customScoring").asInstanceOf[String])
    val summaryFeedback = if (b.get("summaryFeedback") == null) "" else b.get("summaryFeedback").asInstanceOf[String]
    val xhtml = if (b.get("xhtml") == null) "" else b.get("xhtml").asInstanceOf[String]
    new PlayerDefinition(
      files,
      xhtml,
      components,
      summaryFeedback,
      customScoring,
      config)
  } catch {
    case e: Throwable => {
      logger.error(e.getMessage)
      throw PlayerDefinitionTransformerException(e)
    }
  }
}
