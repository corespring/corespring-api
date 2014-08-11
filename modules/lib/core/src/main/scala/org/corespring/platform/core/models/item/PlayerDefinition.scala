package org.corespring.platform.core.models.item

import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import com.mongodb.{ BasicDBList, DBObject }
import com.novus.salat.Context
import com.novus.salat.transformers.CustomTransformer
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.corespring.platform.core.models.item.resource.BaseFile
import org.corespring.play.json.salat.utils.{ ToJsValue, ToDBObject }
import play.api.libs.json._
import org.slf4j.LoggerFactory

/**
 * Model to contain the new v2 player model
 * Note: this is not a case class as we need to support custom serialization w/ salat
 * @param files
 * @param xhtml
 * @param components
 * @param summaryFeedback
 */

object PlayerDefinition {
  def apply(
    files: Seq[BaseFile],
    xhtml: String,
    components: JsValue,
    summaryFeedback: String,
    customScoring: Option[String]) = new PlayerDefinition(files, xhtml, components, summaryFeedback, customScoring)

  implicit object Format extends Format[PlayerDefinition] {
    override def writes(o: PlayerDefinition): JsValue = {
      Json.obj(
        "xhtml" -> o.xhtml,
        "files" -> o.files.map(f => Json.toJson(f)),
        "components" -> o.components,
        "summaryFeedback" -> o.summaryFeedback) ++ o.customScoring.map { cs => Json.obj("customScoring" -> cs) }.getOrElse(Json.obj())
    }

    override def reads(json: JsValue): JsResult[PlayerDefinition] = json match {
      case obj: JsObject => {
        JsSuccess(new PlayerDefinition(
          (json \ "files").asOpt[Seq[BaseFile]].getOrElse(Seq.empty),
          (json \ "xhtml").as[String],
          (json \ "components").asOpt[JsValue].getOrElse(Json.obj()),
          (json \ "summaryFeedback").asOpt[String].getOrElse(""),
          (json \ "customScoring").asOpt[String]))
      }
      case _ => JsError("empty object")
    }
  }

}

class PlayerDefinition(val files: Seq[BaseFile], val xhtml: String, val components: JsValue, val summaryFeedback: String, val customScoring: Option[String]) {
  override def toString = s"""PlayerDefinition(${files}, $xhtml, ${Json.stringify(components)}, $summaryFeedback"""

  override def hashCode() = {
    new HashCodeBuilder(17, 31)
      .append(files)
      .append(xhtml)
      .append(components)
      .append(summaryFeedback)
      .append(customScoring)
      .toHashCode
  }

  override def equals(other: Any) = other match {
    case p: PlayerDefinition => p.files == files && p.xhtml == xhtml && p.components.equals(components) && p.summaryFeedback == summaryFeedback && p.customScoring == customScoring
    case _ => false
  }
}

case class PlayerDefinitionTransformerException(e: Throwable) extends RuntimeException(e)

/**
 * A transformer to help salat to (de)serialize - jsvalue to and from mongo db
 */
class PlayerDefinitionTransformer(val ctx: Context) extends CustomTransformer[PlayerDefinition, DBObject] {

  lazy val logger = LoggerFactory.getLogger(this.getClass.getName)

  import com.novus.salat.grater

  override def serialize(a: PlayerDefinition): DBObject = try {
    logger.trace(s"serialize: ${a}")

    val builder = MongoDBObject.newBuilder

    val preppedFiles: Seq[DBObject] = a.files.map {
      f => grater[BaseFile](ctx, manifest[BaseFile]).asDBObject(f)
    }

    builder += "files" -> MongoDBList(preppedFiles: _*)
    builder += "xhtml" -> a.xhtml
    builder += "components" -> ToDBObject(a.components)
    builder += "summaryFeedback" -> a.summaryFeedback
    builder += "customScoring" -> a.customScoring
    builder.result()
  } catch {
    case e: Throwable => {
      logger.error(e.getMessage)
      throw PlayerDefinitionTransformerException(e)
    }
  }

  override def deserialize(b: DBObject): PlayerDefinition = try {

    logger.trace(s"deserialize: ${b}")

    val json: JsValue = ToJsValue(b.get("components"))
    import com.mongodb.casbah.Implicits._

    val l = b.get("files").asInstanceOf[BasicDBList]
    val prepped: Seq[BaseFile] = l.toList.map {
      dbo => grater[BaseFile](ctx, manifest[BaseFile]).asObject(dbo.asInstanceOf[DBObject])
    }

    val customScoring = if (b.get("customScoring") != null) Some(b.get("customScoring").asInstanceOf[String]) else None

    new PlayerDefinition(
      prepped,
      b.get("xhtml").asInstanceOf[String],
      json,
      b.get("summaryFeedback").asInstanceOf[String],
      customScoring)
  } catch {
    case e: Throwable => {
      logger.error(e.getMessage)
      throw PlayerDefinitionTransformerException(e)
    }
  }
}
