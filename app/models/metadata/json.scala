package models.metadata

import models.{Metadata, MetadataSet}
import play.api.libs.json._


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