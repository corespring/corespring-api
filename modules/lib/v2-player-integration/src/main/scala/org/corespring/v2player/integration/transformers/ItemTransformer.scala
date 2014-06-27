package org.corespring.v2player.integration.transformers

import org.corespring.common.json.JsonTransformer
import org.corespring.platform.core.models.Standard
import org.corespring.platform.core.models.item.resource.{CDataHandler, Resource, VirtualFile}
import org.corespring.platform.core.models.item.{Item, ItemTransformationCache}
import org.corespring.qtiToV2.QtiTransformer
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

import scala.xml.Node

trait ItemTransformer {

  def cache: ItemTransformationCache

  def transformToV2Json(item: Item): JsValue = {
    implicit val ResourceFormat = Resource.Format

    val rootJson: JsObject = item.playerDefinition.map(Json.toJson(_).as[JsObject]).getOrElse(createFromQti(item))
    val profile = toProfile(item)
    rootJson ++ Json.obj(
      "profile" -> profile,
      "supportingMaterials" -> Json.toJson(item.supportingMaterials))
  }

  private def toProfile(item: Item): JsValue = {
    val taskInfoJson = item.taskInfo.map { info =>
      Json.obj("taskInfo" -> mapTaskInfo(Json.toJson(info)))
    }.getOrElse(Json.obj("taskInfo" -> Json.obj()))
    val contributorDetails = item.contributorDetails.map { details =>
      Json.obj("contributorDetails" -> Json.toJson(details))
    }.getOrElse(Json.obj("contributorDetails" -> Json.obj()))
    val otherAlignments = item.otherAlignments.map { alignments =>
      Json.obj("otherAlignments" -> Json.toJson(alignments))
    }.getOrElse(Json.obj("otherAlignments" -> Json.obj()))

    taskInfoJson ++
      contributorDetails ++
      otherAlignments ++
      Json.obj(
        "standards" -> item.standards.map(Standard.findOneByDotNotation).flatten.map(Json.toJson(_)),
        "reviewsPassed" -> item.reviewsPassed,
        "reviewsPassedOther" -> item.reviewsPassedOther,
        "priorGradeLevel" -> item.priorGradeLevels,
        "priorUse" -> item.priorUse,
        "priorUseOther" -> item.priorUseOther,
        "lexile" -> item.lexile
      )
  }

  def mapTaskInfo(taskInfoJson: JsValue): JsValue = {
    val tf = new JsonTransformer(
      "primarySubject" -> "subjects.primary",
      "relatedSubject" -> "subjects.related") {}
    tf.transform(taskInfoJson)
  }

  private def createFromQti(item: Item): JsObject = {
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

  private def getTransformation(item: Item): (Node, JsObject) =
    cache.getCachedTransformation(item) match {
      case Some((node: Node, json: JsValue)) => (node, json.as[JsObject])
      case _ => {
        val qti = for {
          data <- item.data
          qti <- data.files.find(_.name == "qti.xml")
        } yield qti.asInstanceOf[VirtualFile]

        require(qti.isDefined, s"item: ${item.id} has no qti xml")

        val (node, json) = QtiTransformer.transform(scala.xml.XML.loadString(CDataHandler.addCDataTags(qti.get.content)))
        cache.setCachedTransformation(item, (node, json))

        (node, json.as[JsObject])
      }
    }

}
