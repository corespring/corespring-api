package org.corespring.platform.core.models.item.resource

import org.corespring.common.log.PackageLogging
import play.api.libs.json._

/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(name: String, var files: Seq[BaseFile]) {

  def defaultFile = files.find(_.isMain)

}

object Resource extends PackageLogging {
  val name = "name"
  val files = "files"

  val DataPath = "data"

  implicit object Format extends Format[Resource] {
    def writes(res: Resource): JsValue = {
      JsObject(List(
        "name" -> JsString(res.name),
        "files" -> Json.toJson(res.files)))
    }

    def reads(json: JsValue): JsResult[Resource] = {

      json match {
        case obj: JsObject => {
          logger.debug(s"ResourceReads ${Json.prettyPrint(json)}")
          val resourceName = (json \ "name").as[String]
          val files = (json \ "files").asOpt[Seq[BaseFile]]
          JsSuccess(Resource(resourceName, files.getOrElse(Seq())))
        }
        case _ => JsError("Undefined json")
      }
    }
  }
}

