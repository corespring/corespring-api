package org.corespring.platform.core.services.metadata

import play.api.libs.json._
import org.corespring.platform.core.models.metadata.{Metadata, MetadataSet}


object SetJson{
  def apply(set:MetadataSet,data:Option[Metadata]) : JsValue = {
    val setJson = Json.toJson(set)

    val dataJson = data.map{ d =>
      val keys = d.properties.toSeq.map( t => t._1 -> JsString(t._2))
      JsObject(Seq("data" -> JsObject(keys)))
    }.getOrElse(JsObject(Seq()))

    setJson.asInstanceOf[JsObject] ++ dataJson
  }
}