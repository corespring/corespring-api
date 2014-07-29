package org.corespring.dev.tools.controllers

import play.api.libs.json.{JsString, Json}
import play.api.mvc.Controller
import org.corespring.qtiToV2._

object TransformerTestPage extends Controller {

  def testPage = DevToolsAction { request =>
    val call = org.corespring.dev.tools.controllers.routes.TransformerTestPage.transform
    Ok(org.corespring.dev.tools.views.html.TransformerTestPage(call.url))
  }

  def transform = DevToolsAction { request =>
    val inputQtiText = request.body.asText.get
    val inputXml = scala.xml.XML.loadString(inputQtiText)
    val transformed = QtiTransformer.transform(inputXml)
    Ok(Json.obj("xhtml" -> JsString(transformed._1.toString), "data" -> transformed._2))
  }

}
