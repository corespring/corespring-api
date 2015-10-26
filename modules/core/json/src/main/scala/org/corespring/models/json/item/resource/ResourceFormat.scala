package org.corespring.models.json.item.resource

import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ BaseFile, Resource }
import org.corespring.models.json.JsonUtil
import play.api.libs.json._

object ResourceFormat extends JsonUtil with Format[Resource] {
  implicit val bf: Format[BaseFile] = BaseFileFormat

  val id = "id"
  val name = "name"
  val materialType = "materialType"
  val files = "files"

  val DataPath = "data"

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
        val resourceName = (json \ "name").as[String]
        val resourceId = (json \ id).asOpt[String].map(new ObjectId(_)).orElse(Some(ObjectId.get))
        val resourceMaterialType = (json \ materialType).asOpt[String]
        val files = (json \ "files").asOpt[Seq[BaseFile]]
        JsSuccess(Resource(resourceId, resourceName, resourceMaterialType, files.getOrElse(Seq())))
      }
      case _ => JsError("Undefined json")
    }
  }

}
