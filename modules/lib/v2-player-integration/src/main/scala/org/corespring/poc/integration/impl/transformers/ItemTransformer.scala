package org.corespring.poc.integration.impl.transformers

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.VirtualFile
import org.corespring.poc.integration.impl.transformers.qti.QtiTransformer
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

object ItemTransformer {

  def transformToV2Json(item:Item) : JsValue = {

    val qti = item.data.get.files.find( _.name == "qti.xml").getOrElse(throw new RuntimeException("No qti..")).asInstanceOf[VirtualFile].content

    val (xhtml, components) = QtiTransformer.transform(scala.xml.XML.loadString(qti))
    Json.obj(
      "metadata" -> Json.obj(
        "title" -> JsString(item.taskInfo.map(_.title.getOrElse("?")).getOrElse("?"))
      ),
      "files" -> (item.data match {
        case Some(data) => data.files
          .filter(f => f.name != "qti.xml")
          .map(f => Json.obj("name" -> f.name, "contentType" -> f.contentType))
        case _ => Seq.empty[JsObject]
      }),
      "xhtml" -> JsString( xhtml.toString ),
      "components" -> components
    )
  }
}
