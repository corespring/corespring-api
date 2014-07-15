package org.corespring.qtiToV2.transformers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.models.item.resource.{Resource, BaseFile}
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

  def profile(item: Item, profileJson: JsValue): Item = {
    val newContributorDetails = contributorDetails(profileJson).orElse(item.contributorDetails)
    val newInfo = taskInfo(item, profileJson).orElse(item.taskInfo)
    val newLexile = (profileJson \ "lexile").asOpt[String].orElse(item.lexile)
    val newOtherAlignments = otherAlignments(profileJson).orElse(item.otherAlignments)
    val newPriorGradeLevels = (profileJson \ "priorGradeLevel").asOpt[Seq[String]].getOrElse(item.priorGradeLevels)
    val newPriorUse = (profileJson \ "priorUse").asOpt[String].orElse(item.priorUse)
    val newPriorUseOther = (profileJson \ "priorUseOther").asOpt[String].orElse(item.priorUseOther)
    val newReviewsPassed = (profileJson \ "reviewsPassed").asOpt[Seq[String]].getOrElse(item.reviewsPassed)
    val newReviewsPassedOther = (profileJson \ "reviewsPassedOther").asOpt[String].orElse(item.reviewsPassedOther)
    val newStandards = standards(profileJson).getOrElse(item.standards)

    item.copy(
      contributorDetails = newContributorDetails,
      lexile = newLexile,
      otherAlignments = newOtherAlignments,
      priorGradeLevels = newPriorGradeLevels,
      priorUse = newPriorUse,
      priorUseOther = newPriorUseOther,
      reviewsPassed = newReviewsPassed,
      reviewsPassedOther = newReviewsPassedOther,
      standards = newStandards,
      taskInfo = newInfo)
  }

  /**
   * We store the dotNotation only, could also use the id, but the dotNotation
   * is easier to debug
   * @param profileJson
   * @return
   */
  def standards(profileJson: JsValue): Option[Seq[String]] =
    (profileJson\ "standards") match {
      case undefined: JsUndefined => None
      case array: JsArray => Some(array.as[List[JsObject]].map { standard: JsObject =>
        (standard \ "dotNotation").as[String]
      })
      case _ => throw new IllegalArgumentException("additionalCopyrights must be an array")
    }

  def taskInfo(item: Item, profileJson: JsValue): Option[TaskInfo] =
    (profileJson \ "taskInfo").asOpt[JsValue].map { infoJson =>
      val info = item.taskInfo.getOrElse(TaskInfo())

      info.copy(
        description = (infoJson \ "description").asOpt[String],
        gradeLevel = (infoJson \ "gradeLevel").asOpt[Seq[String]].getOrElse(Seq.empty),
        itemType = (infoJson \ "itemType").asOpt[String],
        subjects = subjects(infoJson).orElse(info.subjects),
        title = (infoJson \ "title").asOpt[String].orElse(info.title))
    }

  def subjects(taskInfoJson: JsValue): Option[Subjects] =
    (taskInfoJson \ "subjects").asOpt[JsValue].map { subjects =>
      Subjects(
        primary = (subjects \ "primary" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)),
        related = (subjects \ "related" \ "id").asOpt[String].filter(ObjectId.isValid(_)).map(new ObjectId(_)))
    }

  def contributorDetails(profileJson: JsValue): Option[ContributorDetails] =
    (profileJson \ "contributorDetails").asOpt[JsValue].map { details =>
      ContributorDetails(
        additionalCopyrights = additionalCopyrights(details).getOrElse(Seq()),
        author = (details \ "author").asOpt[String],
        contributor = (details \ "contributor").asOpt[String],
        copyright = Some(copyright(details)),
        costForResource = (details \ "costForResource").asOpt[Int],
        credentials = (details \ "credentials").asOpt[String],
        credentialsOther = (details \ "credentialsOther").asOpt[String],
        licenseType = (details \ "licenseType").asOpt[String],
        sourceUrl = (details \ "sourceUrl").asOpt[String])
    }

  def copyright(contributorDetailsJson: JsValue): Copyright =
    Copyright(
      owner = (contributorDetailsJson \ "copyrightOwner").asOpt[String],
      year = (contributorDetailsJson \ "copyrightYear").asOpt[String],
      expirationDate = (contributorDetailsJson \ "copyrightExpirationDate").asOpt[String])

  def additionalCopyrights(contributorDetailsJson: JsValue): Option[Seq[AdditionalCopyright]] =
    (contributorDetailsJson \ "additionalCopyrights") match {
      case undefined: JsUndefined => None
      case array: JsArray => Some(array.as[List[JsObject]].map(copyright => AdditionalCopyright(
        author = (copyright \ "author").asOpt[String],
        licenseType = (copyright \ "licenseType").asOpt[String],
        mediaType = (copyright \ "mediaType").asOpt[String],
        owner = (copyright \ "owner").asOpt[String],
        sourceUrl = (copyright \ "sourceUrl").asOpt[String],
        year = (copyright \ "year").asOpt[String])))
      case _ => throw new IllegalArgumentException("additionalCopyrights must be an array")
    }

  def otherAlignments(profileJson: JsValue): Option[Alignments] =
    (profileJson \ "otherAlignments").asOpt[JsValue].map { alignments =>
      Alignments(
        bloomsTaxonomy = (alignments \ "bloomsTaxonomy").asOpt[String],
        depthOfKnowledge = (alignments \ "depthOfKnowledge").asOpt[String],
        keySkills = (alignments \ "keySkills").asOpt[Seq[String]].getOrElse(Seq()))
    }

  def supportingMaterials(item: Item, json: JsValue): Item = {
    implicit val baseFileFormat = BaseFile.BaseFileFormat
    (json \ "supportingMaterials") match {
      case undefined: JsUndefined => item
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

