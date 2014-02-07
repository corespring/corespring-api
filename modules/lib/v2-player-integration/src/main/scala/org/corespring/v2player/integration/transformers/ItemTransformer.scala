package org.corespring.v2player.integration.transformers

import org.corespring.platform.core.models.item.{ItemTransformationCache, Item}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import scala.xml.Node
import org.corespring.platform.core.models.item.resource.{CDataHandler, VirtualFile}
import org.corespring.v2player.integration.transformers.qti.QtiTransformer
import play.api.Logger

object ItemTransformer extends ItemTransformationCache {

  def transformToV2Json(item: Item): JsValue = {
    item.playerDefinition.map(Json.toJson(_)).getOrElse(createFromQti(item))
  }

  private def createFromQti(item: Item): JsValue = {
    val (xhtml, components) = getTransformation(item)

    Json.obj(
      "metadata" -> Json.obj(
        "title" -> JsString(item.taskInfo.map(_.title.getOrElse("?")).getOrElse("?"))),
      "files" -> (item.data match {
        case Some(data) => data.files
          .filter(f => f.name != "qti.xml")
          .map(f => Json.obj("name" -> f.name, "contentType" -> f.contentType))
        case _ => Seq.empty[JsObject]
      }),
      "xhtml" -> JsString(xhtml.toString),
      "components" -> components)
  }

  private def getTransformation(item: Item): (Node, JsValue) =
    getCachedTransformation(item) match {
      case Some((node: Node, json: JsValue)) => (node, json)
      case _ => {
        val qti = for {
          data <- item.data
          qti <- data.files.find(_.name == "qti.xml")
        } yield qti.asInstanceOf[VirtualFile]

        require(qti.isDefined, s"item: ${item.id} has no qti xml")

        val (node, json) = QtiTransformer.transform(scala.xml.XML.loadString(CDataHandler.addCDataTags(qti.get.content)))
        setCachedTransformation(item, (node, json))

        (node, json)
      }
    }

}
