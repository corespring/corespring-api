package models.item.resource

import com.novus.salat.annotations.raw.Salat
import play.api.libs.json._
import play.api.libs.json.JsObject
import scala.Some

@Salat
abstract class BaseFile(val name: String, val contentType: String, val isMain: Boolean)

object BaseFile {

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
  }

  val SuffixToContentTypes = Map(
    "jpg" -> ContentTypes.JPG,
    "jpeg" -> ContentTypes.JPG,
    "png" -> ContentTypes.PNG,
    "gif" -> ContentTypes.GIF,
    "doc" -> ContentTypes.DOC,
    "pdf" -> ContentTypes.PDF,
    "xml" -> ContentTypes.XML,
    "css" -> ContentTypes.CSS,
    "html" -> ContentTypes.HTML,
    "txt" -> ContentTypes.TXT,
    "js" -> ContentTypes.JS)

  def getContentType(filename: String): String = {
    val split = filename.split("\\.").toList
    val suffix = split.last
    SuffixToContentTypes.getOrElse(suffix, "unknown")
  }

  implicit object BaseFileWrites extends Writes[BaseFile] {
    def writes(f: BaseFile): JsValue = {
      if (f.isInstanceOf[VirtualFile]) {
        VirtualFile.VirtualFileWrites.writes(f.asInstanceOf[VirtualFile])
      } else {
        StoredFile.StoredFileWrites.writes(f.asInstanceOf[StoredFile])
      }
    }
  }

  implicit object BaseFileReads extends Reads[BaseFile] {

    def reads(json: JsValue): BaseFile = {

      val name = (json \ "name").asOpt[String].getOrElse("unknown")
      val contentType = (json \ "contentType").asOpt[String].getOrElse(getContentType(name))
      val isMain = (json \ "default").asOpt[Boolean].getOrElse(false)


      (json \ "content").asOpt[String] match {
        case Some(content) => {
          VirtualFile(name, contentType, isMain, content)
        }
        case _ => StoredFile(name, contentType, isMain) //we are missing the storageKey here
      }
    }
  }

  def toJson(f: BaseFile): JsObject = {
    JsObject(Seq(
      "name" -> JsString(f.name),
      "contentType" -> JsString(f.contentType),
      "default" -> JsBoolean(f.isMain))
    )
  }
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

  implicit object StoredFileWrites extends Writes[StoredFile] {
    def writes(f: StoredFile): JsValue = {
      BaseFile.toJson(f)
      //"storageKey is for internal use only"
      //++ JsObject(Seq("storageKey" -> JsString(f.storageKey)))
    }
  }
}
