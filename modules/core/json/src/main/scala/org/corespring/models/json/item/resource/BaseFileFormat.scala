package org.corespring.models.json.item.resource

import org.corespring.models.item.resource.{ BaseFile, StoredFile, VirtualFile }
import org.corespring.models.json.JsonUtil
import play.api.libs.json._

object BaseFileFormat extends JsonUtil with Format[BaseFile] {

  def writes(f: BaseFile): JsValue = {
    if (f.isInstanceOf[VirtualFile]) {
      VirtualFileWrites.writes(f.asInstanceOf[VirtualFile])
    } else {
      StoredFileWrites.writes(f.asInstanceOf[StoredFile])
    }
  }

  def reads(json: JsValue): JsResult[BaseFile] = {

    val name = (json \ "name").asOpt[String].getOrElse("unknown")
    val contentType = (json \ "contentType")
      .asOpt[String]
      .filter(BaseFile.isValidContentType(_))
      .getOrElse(BaseFile.getContentType(name))

    //TODO: "default" is the old name, check that it's no longer sent in json then remove.
    val isMain = (json \ "isMain").asOpt[Boolean].orElse((json \ "default").asOpt[Boolean]).getOrElse(false)

    val isTextType = BaseFile.ContentTypes.textTypes.contains(contentType)
    JsSuccess(
      if (isTextType) {
        VirtualFile(name, contentType, isMain, (json \ "content").asOpt[String].getOrElse(""))
      } else {
        StoredFile(name, contentType, isMain)
      })

  }

  def toJson(f: BaseFile): JsObject = Json.obj(
    "name" -> JsString(f.name),
    "contentType" -> JsString(f.contentType),
    "isMain" -> JsBoolean(f.isMain))
}

object VirtualFileWrites extends Writes[VirtualFile] {
  def writes(f: VirtualFile): JsValue = {
    BaseFileFormat.toJson(f) ++ JsObject(Seq("content" -> JsString(f.content)))
  }
}

object StoredFileWrites extends Writes[StoredFile] {
  def writes(f: StoredFile): JsValue = {
    BaseFileFormat.toJson(f)
    //"storageKey is for internal use only"
    //++ JsObject(Seq("storageKey" -> JsString(f.storageKey)))
  }
}
