package org.corespring.v2player.integration.transformers.container

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource }
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition, Subjects, TaskInfo }
import play.api.libs.json._

object PlayerJsonToItem {

  def playerDef(item: Item, playerJson: JsValue): Item = {
    (playerJson \ "xhtml") match {
      case undefined: JsUndefined => item
      case _ => {
        val playerDef = playerJson.as[PlayerDefinition]
        item.copy(playerDefinition = Some(playerDef))
      }
    }
  }

  def profile(item: Item, profileJson: JsValue): Item = (profileJson \ "taskInfo").asOpt[JsValue].map {
    infoJson =>
      val info = item.taskInfo.getOrElse(TaskInfo())

      val subjects = info.subjects.getOrElse(Subjects()).copy(
        primary = (infoJson \ "primarySubject" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)),
        related = (infoJson \ "relatedSubject" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)))

      val newInfo = info.copy(
        title = (infoJson \ "title").asOpt[String].orElse(info.title),
        description = (infoJson \ "description").asOpt[String],
        gradeLevel = (infoJson \ "gradeLevel").asOpt[Seq[String]].getOrElse(Seq.empty),
        subjects = Some(subjects),
        itemType = (infoJson \ "itemType").asOpt[String])
      item.copy(taskInfo = Some(newInfo))
  }.getOrElse(item)

  def supportingMaterials(item: Item, json: JsValue): Item = {
    implicit val baseFileFormat = BaseFile.BaseFileFormat
    (json \ "supportingMaterials") match {
      case undefined: JsUndefined => item
      case _ => (json \ "supportingMaterials") match {
        case array: JsArray => item.copy(
          supportingMaterials =
            array.as[List[JsObject]].map(supportingMaterial => Resource(
              id = (supportingMaterial \ "id").asOpt[String].map(new ObjectId(_)),
              name = (supportingMaterial \ "name").as[String],
              materialType = (supportingMaterial \ "materialType").asOpt[String],
              files = (supportingMaterial \ "files").asOpt[List[JsObject]].getOrElse(List.empty[JsObject])
                .map(f => Json.fromJson[BaseFile](f).get))).map(m => (m.id match {
              case Some(id) => m
              case None => m.copy(id = Some(new ObjectId()))
            })))
        case _ => throw new IllegalArgumentException("supportingMaterials must be an array")
      }
    }
  }

}

