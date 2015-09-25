package org.corespring.models.item.resource

import java.io.InputStream

import com.novus.salat.annotations.Salat
import org.bson.types.ObjectId


//TODO - remove this
@Salat
abstract class BaseFile(
  val name: String,
  val contentType: String,
  val isMain: Boolean)

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
}

/*
 * A VirtualFile is a representation of a file, but the file contents are stored in mongo.
 * Used for text based files.
 */
case class VirtualFile(
  override val name: String,
  override val contentType: String,
  override val isMain: Boolean = false,
  val content: String) extends BaseFile(name, contentType, isMain)

/**
 * A File that has been stored in a file storage service.
 */
case class StoredFile(
  override val name: String,
  override val contentType: String,
  override val isMain: Boolean = false,
  val storageKey: String = "") extends BaseFile(name, contentType, isMain)

object StoredFile {

  def storageKey(id: ObjectId, version: Long, resource: Resource, filename: String): String = {
    storageKey(id, version, resource.name, filename)
  }

  def storageKey(id: ObjectId, version: Long, resourceName: String, filename: String): String = {
    (Seq(id, version.toString) ++ Seq(resourceName, filename)).mkString("/")
  }
}

case class StoredFileDataStream(name: String, stream: InputStream, contentLength: Long, contentType: String, metadata: Map[String, String])

case class CloneResourceResult(files: Seq[CloneFileResult])

trait CloneFileResult {
  def file: StoredFile
  def successful: Boolean
}

case class CloneFileSuccess(val file: StoredFile, s3Key: String) extends CloneFileResult {
  override def successful: Boolean = true
}

case class CloneFileFailure(val file: StoredFile, err: Throwable) extends CloneFileResult {
  override def successful: Boolean = false
}

