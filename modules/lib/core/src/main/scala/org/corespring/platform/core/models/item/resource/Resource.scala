package org.corespring.platform.core.models.item.resource

import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._

/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(id: Option[ObjectId] = None, name: String, materialType: Option[String] = None, var files: Seq[BaseFile]) {
  def defaultFile = files.find(_.isMain)
}

object Resource extends PackageLogging with JsonUtil {
  val id = "id"
  val name = "name"
  val materialType = "materialType"
  val files = "files"

  val DataPath = "data"

  implicit object Format extends Format[Resource] {

    def writes(res: Resource): JsValue = partialObj(
      id -> (res.id match {
        case Some(id) => Some(JsString(id.toString))
        case _ => None
      }),
      name -> Some(JsString(res.name)),
      materialType -> (res.materialType match {
        case Some(materialType) => Some(JsString(materialType))
        case _ => None
      }),
      files -> Some(Json.toJson(res.files)))

    def reads(json: JsValue): JsResult[Resource] = {

      json match {
        case obj: JsObject => {
          logger.debug(s"ResourceReads ${Json.prettyPrint(json)}")
          val resourceName = (json \ "name").as[String]
          val resourceId = (json \ id).asOpt[String].map(new ObjectId(_))
          val resourceMaterialType = (json \ materialType).asOpt[String]
          val files = (json \ "files").asOpt[Seq[BaseFile]]
          JsSuccess(Resource(resourceId, resourceName, resourceMaterialType, files.getOrElse(Seq())))
        }
        case _ => JsError("Undefined json")
      }
    }

  }

}

