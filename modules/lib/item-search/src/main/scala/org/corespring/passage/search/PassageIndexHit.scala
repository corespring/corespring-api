package org.corespring.passage.search

import org.corespring.models.json.JsonUtil
import play.api.Logger
import play.api.libs.json._

case class PassageIndexHit(id: String,
  collectionId: Option[String],
  body: Option[String],
  contentType: String)

object PassageIndexHit {

  object Format extends Format[PassageIndexHit] with JsonUtil {

    val logger = Logger(classOf[PassageIndexHit])


    override def reads(json: JsValue): JsResult[PassageIndexHit] = try {
      JsSuccess(PassageIndexHit(
        id = s"${(json \ "_id").as[String]}${
          (json \ "_source" \ "version").asOpt[Int] match {
            case Some(version) => s":$version"
            case _ => ""
          }
        }",
        collectionId = (json \ "_source" \ "collectionId").asOpt[String],
        body = (json \ "_source" \ "body").asOpt[String],
        contentType = (json \ "_source" \ "contentType").as[String]))
    } catch {
      case t: Throwable => throw t
      case exception: Exception => {
        logger.error(s"Error parsing ${json \ "_id"} $json")
        JsError(s"Error parsing ${json \ "_id"}")

      }
    }

    def writes(passageIndexHit: PassageIndexHit) = {
      import passageIndexHit._
      partialObj(
        "id" -> Some(JsString(id)),
        "collectionId" -> collectionId.map(JsString(_)),
        "body" -> body.map(JsString(_)),
        "contentType" -> Some(JsString(contentType))
      )
    }

  }

}