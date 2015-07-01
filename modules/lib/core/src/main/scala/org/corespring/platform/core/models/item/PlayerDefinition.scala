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
 */

object PlayerDefinition {

  def apply(xhtml: String) = new PlayerDefinition(Seq.empty, xhtml, Json.obj(), "", None)

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

  def empty = PlayerDefinition(Seq(), "", Json.obj(), "", None)

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

  def itemTypes: Map[String, Int] = components match {
    case jsObject: JsObject => jsObject.keys.toSeq.map(components \ _).map(f => (f \ "componentType").asOpt[String])
      .flatten
      .foldLeft(Map[String,Int]() withDefaultValue 0) {
        (acc, componentType) => acc + (componentType -> (1 + acc(componentType)))
      }
    case _ => Map.empty[String, Int]
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

    val json: JsValue = if (b.get("components") == null) Json.obj() else ToJsValue(b.get("components"))

    val files = if (b.get("files") == null || !b.get("files").isInstanceOf[BasicDBList]) {
      Seq.empty
    } else {
      val list = b.get("files").asInstanceOf[BasicDBList]
      list.toList.map {
        dbo => grater[BaseFile](ctx, manifest[BaseFile]).asObject(dbo.asInstanceOf[DBObject])
      }
    }

    val customScoring = if (b.get("customScoring") == null) None else Some(b.get("customScoring").asInstanceOf[String])
    val summaryFeedback = if (b.get("summaryFeedback") == null) "" else b.get("summaryFeedback").asInstanceOf[String]
    val xhtml = if (b.get("xhtml") == null) "" else b.get("xhtml").asInstanceOf[String]
    new PlayerDefinition(
      files,
      xhtml,
      json,
      summaryFeedback,
      customScoring)
  } catch {
    case e: Throwable => {
      logger.error(e.getMessage)
      throw PlayerDefinitionTransformerException(e)
    }
  }
}
