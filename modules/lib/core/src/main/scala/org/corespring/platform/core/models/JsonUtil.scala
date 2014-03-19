package org.corespring.platform.core.models

import play.api.libs.json._

trait JsonUtil {

  def partialObj(fields : (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter{ case (_, v) => v.nonEmpty }.map{ case (a,b) => (a, b.get) })

}
