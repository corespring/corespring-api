package org.corespring.models.json.metadata

import org.corespring.models.metadata.{ Metadata, MetadataSet }
import play.api.libs.json.{ JsObject, JsString, Json, JsValue }

object SetJson {

  implicit val m = MetadataSetFormat

  def apply(set: MetadataSet, data: Option[Metadata]): JsValue = {
    val setJson = Json.toJson(set)

    val dataJson = data.map { d =>
      val keys = d.properties.toSeq.map(t => t._1 -> JsString(t._2))
      JsObject(Seq("data" -> JsObject(keys)))
    }.getOrElse(JsObject(Seq()))

    setJson.asInstanceOf[JsObject] ++ dataJson
  }
}
