package org.corespring.itemSearch

import grizzled.slf4j.Logger
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
  //TODO: move json out of item-search
  object Format extends Format[ItemIndexHit] {

    val logger = Logger(classOf[ItemIndexHit])

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
        id = s"${(json \ "_id").as[String]}${(json \ "_source" \ "version").asOpt[Int] match {
          case Some(version) => s":$version"
          case _ => ""
        }}",
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
case class ItemIndexSearchResult(total: Int, hits: Seq[ItemIndexHit])

object ItemIndexSearchResult {

  object Format extends Format[ItemIndexSearchResult] {

    implicit val ItemIndexHitFormat = ItemIndexHit.Format

    def reads(json: JsValue): JsResult[ItemIndexSearchResult] = {
      JsSuccess(ItemIndexSearchResult(
        total = (json \ "hits" \ "total").asOpt[Int].getOrElse(0),
        hits = (json \ "hits" \ "hits").asOpt[Seq[ItemIndexHit]].getOrElse(Seq.empty)
      ))
    }

    def writes(itemIndexSearchResult: ItemIndexSearchResult): JsValue = Json.obj(
      "total" -> itemIndexSearchResult.total,
      "hits" -> Json.toJson(itemIndexSearchResult.hits)
    )

  }

  def empty = new ItemIndexSearchResult(0, Seq.empty)

}
