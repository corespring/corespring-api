package org.corespring.platform.core.models.item.index

import org.corespring.platform.core.models.JsonUtil
import org.slf4j.LoggerFactory
import play.api.libs.json._

case class ItemIndexHit(id: String,
                        collectionId: Option[String],
                        contributor: Option[String],
                        published: Boolean,
                        standards: Map[String, String],
                        subject: Option[String],
                        gradeLevels: Seq[String],
                        title: Option[String],
                        description: Option[String],
                        apiVersion: Option[Int],
                        itemTypes: Seq[String])

object ItemIndexHit {


  object Format extends Format[ItemIndexHit] with JsonUtil {

    lazy val logger = LoggerFactory.getLogger("ItemIndexHit#Format")

    private def subjectify(json: JsObject) = {
      def emptyOption(string: String): Option[String] = string match {
        case empty if (string.isEmpty) => None
        case _ => Some(string)
      }
      (emptyOption((json \ "category").as[String]), emptyOption((json \ "subject").as[String])) match {
        case (Some(category), Some(subject)) => s"$category: $subject"
        case (None, Some(subject)) => subject
        case (Some(category), None) => category
        case _ => ""
      }
    }

    def reads(json: JsValue) = try {
      JsSuccess(ItemIndexHit(
        id = (json \ "_id").as[String],
        collectionId = (json \ "_source" \ "collectionId").asOpt[String],
        contributor = (json \ "_source" \ "contributorDetails" \ "contributor").asOpt[String],
        published = (json \ "_source" \ "published").asOpt[Boolean].getOrElse(false),
        standards = (json \ "_source" \ "standards").asOpt[Seq[JsObject]].getOrElse(Seq.empty)
          .map(s => (s \ "dotNotation").as[String] -> (s \ "standard").as[String]).toMap,
        subject = (json \ "_source" \ "taskInfo" \ "subjects" \ "primary").asOpt[JsObject].map(subjectify),
        gradeLevels = (json \ "_source" \ "taskInfo" \ "gradeLevel").asOpt[Seq[String]].getOrElse(Seq.empty),
        title = (json \ "_source" \ "taskInfo" \ "title").asOpt[String],
        description = (json \ "_source" \ "taskInfo" \ "description").asOpt[String],
        apiVersion = (json \ "_source" \ "apiVersion").asOpt[Int],
        itemTypes = (json \ "_source" \ "taskInfo" \ "itemTypes").asOpt[Seq[String]].getOrElse(Seq.empty)
      ))
    } catch {
      case exception: Exception => {
        logger.error(s"Error parsing ${json \ "_id"}")
        JsError(s"Error parsing ${json \ "_id"}")
      }
    }

    def writes(indexItemHit: ItemIndexHit) = {
      import indexItemHit._
      Json.obj(
        "id" -> id,
        "collectionId" -> collectionId,
        "contributor" -> contributor,
        "published" -> published,
        "standards" -> standards,
        "subject" -> subject,
        "gradeLevels" -> gradeLevels,
        "title" -> title,
        "description" -> description,
        "apiVersion" -> apiVersion,
        "itemTypes" -> itemTypes
      )
    }
  }

}