package org.corespring.v2player.integration.controllers.editor.json

import play.api.libs.json.JsValue
import org.corespring.platform.core.models.item.{ TaskInfo, PlayerDefinition, Subjects, Item }
import org.bson.types.ObjectId

object PlayerJsonToItem {

  def playerDef(item: Item, playerJson: JsValue): Item = {
    val playerDef = playerJson.as[PlayerDefinition]
    item.copy(playerDefinition = Some(playerDef))
  }

  def profile(item: Item, profileJson: JsValue): Item = (profileJson \ "taskInfo").asOpt[JsValue].map {
    infoJson =>
      val info = item.taskInfo.getOrElse(TaskInfo())

      val subjects = info.subjects.getOrElse(Subjects()).copy(
        primary = (infoJson \ "primarySubject" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)),
        related = (infoJson \ "relatedSubject" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)))

      val newInfo = info.copy(
        title = (infoJson \ "title").asOpt[String].orElse(info.title),
        gradeLevel = (infoJson \ "gradeLevel").as[Seq[String]],
        subjects = Some(subjects),
        itemType = (infoJson \ "itemType").asOpt[String])
      item.copy(taskInfo = Some(newInfo))
  }.getOrElse(item)

}
