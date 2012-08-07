package api

import play.api.libs.json._


/**
*  A helper class to render count results
 */
object CountResult {
  def toJson(count: Int): JsValue = {
    Json.toJson(Map("count" -> count))
  }
}
