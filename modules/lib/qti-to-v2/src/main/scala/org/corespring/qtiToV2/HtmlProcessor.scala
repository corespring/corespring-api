package org.corespring.qtiToV2

import play.api.libs.json.{JsString, JsArray, JsObject, JsValue}

trait HtmlProcessor extends EntityEscaper {

  def preprocessHtml(html: String) = escapeEntities(Windows1252EntityTransformer.transform(html))

  def postprocessHtml(html: String) = unescapeEntities(html)

  def postprocessHtml(jsValue: JsValue): JsValue = jsValue match {
    case jsObject: JsObject => JsObject(jsObject.fields.map{ case (key, value) => (key, postprocessHtml(value)) })
    case jsArray: JsArray => JsArray(jsArray.value.map{ value => postprocessHtml(value) })
    case jsString: JsString => JsString(postprocessHtml(jsString.value))
    case _ => jsValue
  }

}
