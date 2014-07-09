package org.corespring.platform.core.models.item.resource

import com.novus.salat.annotations.raw.Salat
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import org.corespring.platform.core.models.JsonUtil

@Salat
abstract class BaseFile(val name: String, val contentType: String, val isMain: Boolean)

object BaseFile extends JsonUtil {

  object ContentTypes {

    val JPG: String = "image/jpg"
    val PNG: String = "image/png"
    val GIF: String = "image/gif"
    val DOC: String = "application/msword"
    val PDF: String = "application/pdf"
    val XML: String = "text/xml"
    val CSS: String = "text/css"
    val HTML: String = "text/html"
    val TXT: String = "text/txt"
    val JS: String = "text/javascript"
    val JSON: String = "application/json"
    val UNKNOWN: String = "unknown"

    lazy val textTypes = Seq(XML, CSS, HTML, TXT, JS, JSON, UNKNOWN)
    lazy val binaryTypes = Seq(JPG, PNG, GIF, DOC, PDF)
  }

  val SuffixToContentTypes = Map(
    "jpg" -> ContentTypes.JPG,
    "jpeg" -> ContentTypes.JPG,
    "png" -> ContentTypes.PNG,
    "gif" -> ContentTypes.GIF,
    "doc" -> ContentTypes.DOC,
    "docx" -> ContentTypes.DOC,
    "pdf" -> ContentTypes.PDF,
    "xml" -> ContentTypes.XML,
    "css" -> ContentTypes.CSS,
    "html" -> ContentTypes.HTML,
    "txt" -> ContentTypes.TXT,
    "json" -> ContentTypes.JSON,
    "js" -> ContentTypes.JS)

  def getContentType(filename: String): String = {
    val split = filename.split("\\.").toList
    val suffix = split.last.toLowerCase
    SuffixToContentTypes.getOrElse(suffix, ContentTypes.UNKNOWN)
  }

  implicit object BaseFileFormat extends Format[BaseFile] {

    def writes(f: BaseFile): JsValue = {
      if (f.isInstanceOf[VirtualFile]) {
        VirtualFile.VirtualFileWrites.writes(f.asInstanceOf[VirtualFile])
      } else {
        StoredFile.StoredFileWrites.writes(f.asInstanceOf[StoredFile])
      }
    }

    def reads(json: JsValue): JsResult[BaseFile] = {

      val name = (json \ "name").asOpt[String].getOrElse("unknown")
      val contentType = (json \ "contentType").asOpt[String].getOrElse(getContentType(name))
      val isMain = (json \ "default").asOpt[Boolean].getOrElse(false)

      import org.corespring.platform.core.models.item.resource.BaseFile.ContentTypes._

      val isTextType = textTypes.contains(contentType)

      JsSuccess(
        if (isTextType) {
          VirtualFile(name, contentType, isMain, (json \ "content").asOpt[String].getOrElse(""))
        } else {
          StoredFile(name, contentType, isMain)
        })
    }

  }

  def toJson(f: BaseFile): JsObject = Json.obj(
    "name" -> JsString(f.name),
    "contentType" -> JsString(f.contentType),
    "default" -> JsBoolean(f.isMain))
}

/*
 * A VirtualFile is a representation of a file, but the file contents are stored in mongo.
 * Used for text based files.
 */
case class VirtualFile(override val name: String, override val contentType: String, override val isMain: Boolean = false, var content: String) extends BaseFile(name, contentType, isMain)

object VirtualFile {
  val content = "content"

  implicit object VirtualFileWrites extends Writes[VirtualFile] {
    def writes(f: VirtualFile): JsValue = {
      BaseFile.toJson(f) ++ JsObject(Seq(content -> JsString(f.content)))
    }
  }

}

/**
 * A File that has been stored in a file storage service.
 */
case class StoredFile(override val name: String, override val contentType: String, override val isMain: Boolean = false, var storageKey: String = "") extends BaseFile(name, contentType, isMain)

object StoredFile {

  def storageKey(id: VersionedId[ObjectId], resource: Resource, filename: String): String = {
    storageKey(id, resource.name, filename)
  }

  def storageKey(id: VersionedId[ObjectId], resourceName: String, filename: String): String = {
    (Seq(id.id) ++ id.version ++ Seq(resourceName, filename)).mkString("/")
  }

  implicit object StoredFileWrites extends Writes[StoredFile] {
    def writes(f: StoredFile): JsValue = {
      BaseFile.toJson(f)
      //"storageKey is for internal use only"
      //++ JsObject(Seq("storageKey" -> JsString(f.storageKey)))
    }
  }

}
