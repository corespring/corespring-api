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
 */

object PlayerDefinition {
  def apply(files: Seq[BaseFile], xhtml: String, components: JsValue) = new PlayerDefinition(files, xhtml, components)

  implicit object Format extends Format[PlayerDefinition] {
    override def writes(o: PlayerDefinition): JsValue = {
      Json.obj(
        "xhtml" -> o.xhtml,
        "files" -> o.files.map(f => Json.toJson(f)),
        "components" -> o.components)
    }

    override def reads(json: JsValue): JsResult[PlayerDefinition] = json match {
      case obj: JsObject => {
        JsSuccess(new PlayerDefinition(
          (json \ "files").asOpt[Seq[BaseFile]].getOrElse(Seq.empty),
          (json \ "xhtml").as[String],
          (json \ "components").as[JsValue]))
      }
      case _ => JsError("empty object")
    }
  }

}

class PlayerDefinition(val files: Seq[BaseFile], val xhtml: String, val components: JsValue) {
  override def toString = s"""PlayerDefinition(${files}, $xhtml, ${Json.stringify(components)}"""

  override def hashCode() = {
    new HashCodeBuilder(17, 31)
      .append(files)
      .append(xhtml)
      .append(components)
      .toHashCode
  }

  override def equals(other: Any) = other match {
    case p: PlayerDefinition => p.files == files && p.xhtml == xhtml && p.components.equals(components)
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
    builder.result()
  } catch {
    case e: Throwable => throw PlayerDefinitionTransformerException(e)
  }

  override def deserialize(b: DBObject): PlayerDefinition = try {

    logger.trace(s"deserialize: ${b}")

    val json: JsValue = ToJsValue(b.get("components"))
    import com.mongodb.casbah.Implicits._

    val l = b.get("files").asInstanceOf[BasicDBList]
    val prepped: Seq[BaseFile] = l.toList.map {
      dbo => grater[BaseFile](ctx, manifest[BaseFile]).asObject(dbo.asInstanceOf[DBObject])
    }
    new PlayerDefinition(
      prepped,
      b.get("xhtml").asInstanceOf[String],
      json)
  } catch {
    case e: Throwable => throw PlayerDefinitionTransformerException(e)
  }
}
