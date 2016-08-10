package org.corespring.common.json

import play.api.libs.json.{ JsString, JsArray, JsObject, JsValue }

object JsonStringReplace {

  def replaceStringsInJson(json: JsValue)(replace: (String => String)): JsValue = {
    def recurse(input: JsValue): JsValue = {
      if (input.isInstanceOf[JsObject]) {
        JsObject(input.as[JsObject].fields.map(kv => (kv._1, recurse(kv._2))))
      } else if (input.isInstanceOf[JsArray]) {
        JsArray(input.as[JsArray].value.map(v => recurse(v)))
      } else if (input.isInstanceOf[JsString]) {
        JsString(replace(input.as[JsString].value))
      } else {
        input
      }
    }
    recurse(json)
  }
}
