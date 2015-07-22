package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item.Item
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ StandardService, SubjectService }
import play.api.Logger
import play.api.libs.json._

trait ItemToSummaryData {

  def subjectService: SubjectService
  def standardService: StandardService

  def jsonFormatting: JsonFormatting

  implicit val subjectFormat = jsonFormatting.writeSubject
  implicit val standardFormat = jsonFormatting.formatStandard

  lazy val logger = Logger(classOf[ItemToSummaryData])

  def toSummaryData(item: Item, detail: Option[String] = None): JsValue = {
    logger.debug(s"itemId=${item.id} function=transform $detail")

    val normal = Json.obj(
      "id" -> item.id.toString(),
      "author" -> item.contributorDetails.map(details => details.author),
      "title" -> item.taskInfo.map(info => info.title),
      "gradeLevel" -> item.taskInfo.map(info => info.gradeLevel),
      "itemType" -> item.taskInfo.map(info => info.itemType),
      "standards" -> standard(item.standards),
      "priorUse" -> priorUse(item)) ++ subjects(item)

    val details = detail.getOrElse("normal") match {
      case "detailed" => Json.obj(
        "copyrightOwner" -> item.contributorDetails.map(details => details.copyright.map(copyright => copyright.owner)),
        "credentials" -> item.contributorDetails.map(details => details.credentials),
        "keySkills" -> item.otherAlignments.map(alignments => alignments.keySkills),
        "bloomsTaxonomy" -> item.otherAlignments.map(alignments => alignments.bloomsTaxonomy))

      /** Todo what does full really mean? **/
      case "full" => Json.obj(
        "copyrightOwner" -> item.contributorDetails.map(details => details.copyright.map(copyright => copyright.owner)),
        "credentials" -> item.contributorDetails.map(details => details.credentials),
        "keySkills" -> item.otherAlignments.map(alignments => alignments.keySkills),
        "bloomsTaxonomy" -> item.otherAlignments.map(alignments => alignments.bloomsTaxonomy))
      case _ => Json.obj()
    }

    val out = normal ++ details
    logger.trace(s"itemId=${item.id} function=transform $detail json=${Json.stringify(out)}")
    out
  }

  /**
   * return primary and related subject as one object
   * @param item
   * @return
   */
  private def subjects(item: Item): JsObject = {
    def subjectToJson(subject: Option[ObjectId]): Option[JsValue] = subject match {
      case Some(id) => subjectService.findOneById(id) match {
        case Some(subj) => Some(Json.toJson(subj))
        case _ => throw new RuntimeException("Can't find subject with id: " + id)
      }
      case _ => None
    }

    def subjectsToJson(subjects: Seq[ObjectId]): Option[JsArray] = Some(JsArray(
      subjects.map(id => subjectService.findOneById(id) match {
        case Some(subj) => Json.toJson(subj)
        case _ => throw new RuntimeException("Can't find subject with id: " + id)
      })))

    val primaryJson: Option[JsObject] = for {
      info <- item.taskInfo
      subjects <- info.subjects
      json <- subjectToJson(subjects.primary)
    } yield Json.obj("primarySubject" -> json)

    val relatedJson: Option[JsObject] = for {
      info <- item.taskInfo
      subjects <- info.subjects
      json <- subjectsToJson(subjects.related)
    } yield Json.obj("relatedSubject" -> json)

    primaryJson.getOrElse(Json.obj()) ++ relatedJson.getOrElse(Json.obj())
  }

  private def standard(standards: Seq[String]): Seq[JsValue] = {
    standards.map(standardService.findOneByDotNotation).flatten.map(Json.toJson(_))
  }

  private def priorUse(item: Item): JsObject = {
    Json.obj(
      "contributorName" -> item.contributorDetails.map(details => details.contributor),
      "credentials" -> item.contributorDetails.map(details => details.credentials),
      "gradeLevel" -> item.priorGradeLevels,
      "sourceUrl" -> "",
      "reviewsPassed" -> "",
      "use" -> item.priorUse,
      "priorUseOther" -> item.priorUseOther,
      "pValue" -> "") ++ subjects(item)
  }

}
