package org.corespring.dev.tools.controllers

import org.corespring.qtiToV2._
import play.api.mvc.Controller

object TransformerTestPage extends Controller {

  def testPage = DevToolsAction { request =>
    val call = org.corespring.dev.tools.controllers.routes.TransformerTestPage.transform
    Ok(org.corespring.dev.tools.views.html.TransformerTestPage(call.url))
  }

  def transform = DevToolsAction { request =>
    val inputQtiText = request.body.asText.get
    val inputXml = scala.xml.XML.loadString(inputQtiText)
    val transformed = QtiTransformer.transform(inputXml)
    Ok(transformed)
  }

}
