package org.corespring.platform.core.models.item.index

import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._

case class ItemIndexHit(id: String,
                        collectionId: String,
                        contributor: Option[String],
                        published: Boolean,
                        standards: Seq[String],
                        subject: Option[String],
                        gradeLevels: Seq[String],
                        title: Option[String],
                        description: Option[String],
                        apiVersion: Option[Int])

object ItemIndexHit {

  object Format extends Format[ItemIndexHit] with JsonUtil {

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

    def reads(json: JsValue) = JsSuccess(ItemIndexHit(
      id = (json \ "_id").as[String],
      collectionId = (json \ "_source" \ "collectionId").as[String],
      contributor = (json \ "_source" \ "contributorDetails" \ "contributor").asOpt[String],
      published = (json \ "_source" \ "published").asOpt[Boolean].getOrElse(false),
      standards = (json \ "_source" \ "standards").asOpt[Seq[JsObject]].getOrElse(Seq.empty)
        .map(standard => (standard \ "dotNotation").as[String]),
      subject = (json \ "_source" \ "taskInfo" \ "subjects" \ "primary").asOpt[JsObject].map(subjectify),
      gradeLevels = (json \ "_source" \ "taskInfo" \ "gradeLevel").asOpt[Seq[String]].getOrElse(Seq.empty),
      title = (json \ "_source" \ "taskInfo" \ "title").asOpt[String],
      description = (json \ "_source" \ "taskInfo" \ "description").asOpt[String],
      apiVersion = (json \ "_source" \ "apiVersion").asOpt[Int]
    ))

    def writes(indexItemHit: ItemIndexHit) = {
      import indexItemHit._
      Json.obj(
        "id" -> id,
        "collectionId" -> collectionId,
        "published" -> published,
        "standards" -> standards,
        "subject" -> subject,
        "gradeLevels" -> gradeLevels,
        "title" -> title,
        "description" -> description,
        "apiVersion" -> apiVersion
      )
    }
  }

}
