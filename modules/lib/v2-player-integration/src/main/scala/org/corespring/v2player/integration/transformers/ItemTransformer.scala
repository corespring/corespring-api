package org.corespring.v2player.integration.transformers

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.VirtualFile
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import org.corespring.v2player.integration.transformers.qti.QtiTransformer


object ItemTransformer {

  def transformToV2Json(item:Item) : JsValue = {

    val qti = item.data.get.files.find(_.name == "qti.xml").getOrElse(throw new RuntimeException("No qti..")).asInstanceOf[VirtualFile].content

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
      "xhtml" -> JsString(xhtml.toString),
      "components" -> components
    )
  }
}
