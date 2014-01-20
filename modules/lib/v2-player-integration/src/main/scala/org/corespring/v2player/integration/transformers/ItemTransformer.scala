package org.corespring.v2player.integration.transformers

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{CDataHandler, VirtualFile}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import org.corespring.v2player.integration.transformers.qti.QtiTransformer


object ItemTransformer {

  def transformToV2Json(item:Item) : JsValue = {

    val qti = for{
      data <- item.data
      qti <- data.files.find(_.name == "qti.xml")
    } yield qti.asInstanceOf[VirtualFile]

    require(qti.isDefined, s"item: ${item.id} has no qti xml")

    val (xhtml, components) = QtiTransformer.transform(
      scala.xml.XML.loadString(CDataHandler.addCDataTags(qti.get.content)))
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
