package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.{ Standard, Subject }
import org.corespring.v2.log.V2LoggerFactory
import play.api.Logger
import play.api.libs.json._

trait ItemTransformerToSummaryData {

  lazy val logger = V2LoggerFactory.getLogger("api", "V2ItemTransformer")

  def transform(item: Item, detail: Option[String] = None): JsValue = {
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
      case Some(id) => Subject.findOneById(id) match {
        case Some(subj) => Some(Json.toJson(subj))
        case _ => throw new RuntimeException("Can't find subject with id: " + id)
      }
      case _ => None
    }

    val primaryJson: Option[JsObject] = for {
      info <- item.taskInfo
      subjects <- info.subjects
      json <- subjectToJson(subjects.primary)
    } yield Json.obj("primarySubject" -> json)

    val relatedJson: Option[JsObject] = for {
      info <- item.taskInfo
      subjects <- info.subjects
      json <- subjectToJson(subjects.related)
    } yield Json.obj("relatedSubject" -> json)

    primaryJson.getOrElse(Json.obj()) ++ relatedJson.getOrElse(Json.obj())
  }

  private def standard(standards: Seq[String]): Seq[JsValue] = {
    standards.map(Standard.findOneByDotNotation).flatten.map(Json.toJson(_))
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
