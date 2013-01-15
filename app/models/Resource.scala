package models

import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.BasicDBObject
import controllers.{LogType, InternalError}


/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(name: String, var files: Seq[BaseFile])

object Resource extends Searchable{
  val name = "name"
  val files = "files"


  val QtiXml = "qti.xml"

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
          case _ => StoredFile(fileName, contentType, isMain, (f \ "storageKey").asOpt[String].getOrElse(""))
        }
      }))
      Resource(resourceName, files.getOrElse(Seq()))
    }
  }

  def toSearchObj(query: AnyRef):Either[InternalError,MongoDBObject] = {
    query match {
      case dbquery:BasicDBObject =>
      case _ => Left(InternalError("invalid search object in Resource",LogType.printFatal,addMessageToClientOutput = true))
    }
  }
}

