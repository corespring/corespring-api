package org.corespring.qtiToV2.transformers

import org.bson.types.ObjectId
import org.corespring.common.json.JsonTransformer
import org.corespring.platform.core.models.Standard
import org.corespring.platform.core.models.item.resource.{ CDataHandler, Resource, VirtualFile }
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.QtiTransformer
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }

trait ItemTransformer {

  def cache: ItemTransformationCache
  def itemService: ItemService

  def updateV2Json(itemId: VersionedId[ObjectId]): Option[Item] = {
    itemService.findOneById(itemId) match {
      case Some(item) => item.playerDefinition match {
        case None => try {
          updateV2Json(item)
        } catch {
          case e: Exception => {
            e.printStackTrace
            None
          }
        }
        case _ => Some(item)
      }
      case _ => None
    }
  }

  def updateV2Json(item: Item): Option[Item] = {
    transformToV2Json(item, Some(createFromQti(item))).asOpt[PlayerDefinition]
      .map(playerDefinition => item.copy(playerDefinition = Some(playerDefinition))) match {
        case Some(updatedItem) => item.playerDefinition.equals(updatedItem.playerDefinition) match {
          case true => Some(updatedItem)
          case _ => {
            itemService.save(updatedItem)
            Some(updatedItem)
          }
        }
        case _ => None
      }
  }

  def transformToV2Json(item: Item): JsValue = transformToV2Json(item, None)

  def transformToV2Json(item: Item, rootJson: Option[JsObject]): JsValue = {
    implicit val ResourceFormat = Resource.Format

    val root: JsObject = (rootJson match {
      case Some(json) => json
      case None => item.playerDefinition.map(Json.toJson(_).as[JsObject]).getOrElse(createFromQti(item))
    })
    val profile = toProfile(item)
    root ++ Json.obj(
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
        "lexile" -> item.lexile)
  }

  def mapTaskInfo(taskInfoJson: JsValue): JsValue = {
    val tf = new JsonTransformer(
      "primarySubject" -> "subjects.primary",
      "relatedSubject" -> "subjects.related") {}
    tf.transform(taskInfoJson)
  }

  private def createFromQti(item: Item): JsObject = {
    val transformedJson = getTransformation(item)

    Json.obj(
      "metadata" -> Json.obj(
        "title" -> JsString(item.taskInfo.map(_.title.getOrElse("?")).getOrElse("?"))),
      "files" -> (item.data match {
        case Some(data) => data.files
          .filter(f => f.name != "qti.xml")
          .map(f => Json.obj("name" -> f.name, "contentType" -> f.contentType))
        case _ => Seq.empty[JsObject]
      })) ++ transformedJson.as[JsObject]
  }

  private def getTransformation(item: Item): JsValue =
    cache.getCachedTransformation(item) match {
      case Some(json: JsValue) => json
      case _ => {
        val qti = for {
          data <- item.data
          qti <- data.files.find(_.name == "qti.xml")
        } yield qti.asInstanceOf[VirtualFile]

        require(qti.isDefined, s"item: ${item.id} has no qti xml")

        val transformedJson = QtiTransformer.transform(scala.xml.XML.loadString(CDataHandler.addCDataTags(qti.get.content)))
        cache.setCachedTransformation(item, transformedJson)
        transformedJson

      }
    }

}
