package org.corespring.platform.core.models.item.resource

import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some
import org.corespring.common.log.PackageLogging
import org.json4s.JValue

/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(name: String, var files: Seq[BaseFile]) {
  def defaultFile = files.find(_.isMain)
}

object Resource extends PackageLogging {
  val name = "name"
  val files = "files"

  val QtiXml = "qti.xml"
  val QtiPath = "data"

  implicit object ResourceWrites extends Writes[Resource] {
    def writes(res: Resource): JsValue = {
      JsObject(List(
        "name" -> JsString(res.name),
        "files" -> Json.toJson(res.files)))
    }
  }

  implicit object ResourceReads extends Reads[Resource] {
    def reads(json: JsValue): JsResult[Resource] = {

      json match {
        case obj: JsObject => {
          logger.debug(s"ResourceReads ${Json.prettyPrint(json)}")
          val resourceName = (json \ "name").as[String]
          val files = (json \ "files").asOpt[Seq[JsValue]].map(_.map(f => {

            val fileName = (f \ "name").as[String]
            val contentType = (f \ "contentType").as[String]
            val isMain = (f \ "default").as[Boolean]
            (f \ "content").asOpt[String] match {
              case Some(c) => VirtualFile(fileName, contentType, isMain, c)
              case _ => StoredFile(fileName, contentType, isMain, (f \ "storageKey").asOpt[String].getOrElse(""))
            }
          }))
          JsSuccess(Resource(resourceName, files.getOrElse(Seq())))
        }
        case _ => JsError("Undefined json")
      }
    }
  }
}

