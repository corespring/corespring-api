package org.corespring.models.json.item

import org.corespring.models.item.StandardCluster
import play.api.libs.json._

object StandardClusterFormat extends Format[StandardCluster] {

  object Keys {
    val hidden = "hidden"
    val source = "source"
    val text = "text"
  }

  def writes(obj: StandardCluster) = {

    Json.obj(
      Keys.text -> obj.text,
      Keys.hidden -> obj.hidden,
      Keys.source -> obj.source
    )
  }

  def reads(json: JsValue) = {
    try {
      val result = StandardCluster(
        text = (json \ Keys.text).as[String],
        hidden = (json \ Keys.hidden).as[Boolean],
        source = (json \ Keys.source).as[String])
      JsSuccess(result)
    } catch {
      case e: IllegalArgumentException => JsError("error parsing subjects")
    }
  }

}
