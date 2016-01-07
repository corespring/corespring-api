package org.corespring.models.item

import org.bson.types.ObjectId
import org.corespring.models.item.resource.{VirtualFile, BaseFile}
import org.corespring.platform.data.mongo.models.{VersionedId, EntityWithVersionedId}
import play.api.libs.json._

case class Passage(id: VersionedId[ObjectId] = VersionedId(ObjectId.get(), Some(0)),
  contentType: String = Passage.contentType,
  collectionId: String,
  file: BaseFile = Passage.defaultFile) extends Content[VersionedId[ObjectId]] with EntityWithVersionedId[ObjectId] {
}

object Passage {
  val contentType = "passage"

  object Defaults {
    object File {
      val name = "passage.html"
      val contentType = "application/html"
      val content = "<html></html>"
    }
  }

  def defaultFile: BaseFile = new VirtualFile(
    name = Defaults.File.name,
    contentType = Defaults.File.contentType,
    isMain = true,
    content = Defaults.File.content)

  object Format extends Format[Passage] {
    override def reads(json: JsValue): JsResult[Passage] = ???
    override def writes(passage: Passage): JsValue ={
      import passage._

      Json.obj(
        "id" -> id.toString,
        "collectionId" -> collectionId,
        "file" -> (file match {
          case file: VirtualFile => {
            Json.obj(
              "name" -> file.name,
              "contentType" -> file.contentType,
              "content" -> file.content
            )
          }
          case _ => throw new Exception(s"Type ${file.getClass.toString} not supported")
        })
      )
    }
  }

}