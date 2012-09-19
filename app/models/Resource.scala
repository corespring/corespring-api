package models

import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some


/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(name: String, var files: Seq[BaseFile])

object Resource {
  val name = "name"
  val files = "files"

  implicit object ResourceWrites extends Writes[Resource] {
    def writes(res: Resource): JsValue = {
      import BaseFile._
      JsObject(List(
        "name" -> JsString(res.name),
        "files" -> Json.toJson(res.files)
      ))
    }
  }

  implicit object ResourceReads extends Reads[Resource] {
    def reads(json: JsValue): Resource = {
      val resourceName = (json \ "name").as[String]
      val files = (json \ "files").asOpt[Seq[JsValue]].map(_.map(f => {

        val fileName = (f \ "name").as[String]
        val contentType = (f \ "contentType").as[String]
        val isMain = (f \ "default").as[Boolean]
        (f \ "content").asOpt[String] match {
          case Some(c) => VirtualFile(fileName, contentType, isMain, c)
          case _ => StoredFile(fileName, contentType, isMain, (f \ "storageKey").as[String])
        }
      }))
      Resource(resourceName, files.getOrElse(Seq()))
    }
  }
}

