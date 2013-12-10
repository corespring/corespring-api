package org.corespring.poc.integration.impl.transformers

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.VirtualFile
import org.corespring.poc.integration.impl.transformers.qti.QtiTransformer
import play.api.libs.json.{JsString, JsValue, Json}

object ItemTransformer {

  def transformToV2Json(item:Item) : JsValue = {

    val qti = item.data.get.files.find( _.name == "qti.xml").getOrElse(throw new RuntimeException("No qti..")).asInstanceOf[VirtualFile].content

    val (xhtml, components) = QtiTransformer.transform(scala.xml.XML.loadString(qti))
    Json.obj(
      "metadata" -> Json.obj(
        "title" -> JsString(item.taskInfo.map(_.title.getOrElse("?")).getOrElse("?"))
      ),
      "xhtml" -> JsString( xhtml.toString ),
      "components" -> components
    )
  }
}
