package org.corespring.conversion.qti.transformers

import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.models.json.JsonFormatting
import play.api.libs.json._

class PlayerJsonToItem(jsonFormatting: JsonFormatting) {

  import jsonFormatting._

  def wholeItem(item: Item, itemJson: JsValue): Item = {

    val pd: (Item => Item) = playerDef(_, itemJson)
    val pr: (Item => Item) = profile(_, itemJson \ "profile")
    pd.andThen(pr)(item)
  }

  def playerDef(item: Item, playerJson: JsValue): Item = (playerJson \ "xhtml").asOpt[String].map { _ =>
    implicit val pdf = org.corespring.models.json.item.PlayerDefinitionFormat
    val playerDef = playerJson.as[PlayerDefinition]

    val merged = item.playerDefinition.map { pd =>
      pd.mergeAllButFiles(playerDef)
    }.getOrElse(playerDef)
    item.copy(playerDefinition = Some(merged))
  }.getOrElse(item)

  def profile(item: Item, profileJson: JsValue): Item = {

    def standards: Option[Seq[String]] = {
      (profileJson \ "standards")
        .asOpt[Seq[JsValue]].map { s => s.flatMap(v => (v \ "dotNotation").asOpt[String]) }
    }

    def taskInfo: Option[TaskInfo] =
      (profileJson \ "taskInfo").asOpt[TaskInfo].map { ti =>
        val s = subjects(profileJson \ "taskInfo").orElse(ti.subjects)
        ti.copy(subjects = s)
      }

    def subjects(taskInfoJson: JsValue): Option[Subjects] = {
      (taskInfoJson \ "subjects").asOpt[JsValue].map { subjects =>
        Subjects(
          primary = (subjects \ "primary" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)),
          related = (subjects \ "related" \\ "id").map(_.as[String]).filter(ObjectId.isValid(_)).map(new ObjectId(_)))
      }
    }

    val newInfo = taskInfo.orElse(item.taskInfo)
    val newLexile = (profileJson \ "lexile").asOpt[String].orElse(item.lexile)
    val newOtherAlignments = (profileJson \ "otherAlignments").asOpt[Alignments].orElse(item.otherAlignments)
    val newPriorGradeLevels = (profileJson \ "priorGradeLevel").asOpt[Seq[String]].getOrElse(item.priorGradeLevels)
    val newPriorUse = (profileJson \ "priorUse").asOpt[String].orElse(item.priorUse)
    val newPriorUseOther = (profileJson \ "priorUseOther").asOpt[String].orElse(item.priorUseOther)
    val newReviewsPassed = (profileJson \ "reviewsPassed").asOpt[Seq[String]].getOrElse(item.reviewsPassed)
    val newReviewsPassedOther = (profileJson \ "reviewsPassedOther").asOpt[String].orElse(item.reviewsPassedOther)
    val newStandards = standards.getOrElse(item.standards)

    item.copy(
      contributorDetails = (profileJson \ "contributorDetails").asOpt[ContributorDetails], //newContributorDetails,
      lexile = newLexile,
      otherAlignments = newOtherAlignments,
      priorGradeLevels = newPriorGradeLevels,
      priorUse = newPriorUse,
      priorUseOther = newPriorUseOther,
      reviewsPassed = newReviewsPassed,
      reviewsPassedOther = newReviewsPassedOther,
      standards = newStandards,
      taskInfo = newInfo,
      workflow = (profileJson \ "workflow").asOpt[Workflow])
  }
}
