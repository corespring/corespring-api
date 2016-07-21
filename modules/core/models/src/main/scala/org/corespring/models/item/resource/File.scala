package org.corespring.models.item.resource

import java.io.InputStream
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

abstract class BaseFile(
  val name: String,
  val contentType: String,
  val isMain: Boolean)

object BaseFile {

  object ContentTypes {

    val CSS: String = "text/css"
    val DOC: String = "application/msword"
    val GIF: String = "image/gif"
    val HTML: String = "text/html"
    val JPEG: String = "image/jpeg"
    val JS: String = "text/javascript"
    val JSON: String = "application/json"
    val MP3: String = "audio/mp3"
    val OGG: String = "audio/ogg"
    val PDF: String = "application/pdf"
    val PNG: String = "image/png"
    val TXT: String = "text/plain"
    val UNKNOWN: String = "unknown"
    val XML: String = "text/xml"

    lazy val textTypes = Seq(XML, CSS, HTML, TXT, JS, JSON, UNKNOWN)
    lazy val binaryTypes = Seq(DOC, PDF, JPEG, PNG, GIF, MP3, OGG)
  }

  val SuffixToContentTypes = Map(
    "css" -> ContentTypes.CSS,
    "doc" -> ContentTypes.DOC,
    "docx" -> ContentTypes.DOC,
    "gif" -> ContentTypes.GIF,
    "html" -> ContentTypes.HTML,
    "jpeg" -> ContentTypes.JPEG,
    "jpg" -> ContentTypes.JPEG,
    "js" -> ContentTypes.JS,
    "json" -> ContentTypes.JSON,
    "mp3" -> ContentTypes.MP3,
    "ogg" -> ContentTypes.OGG,
    "pdf" -> ContentTypes.PDF,
    "png" -> ContentTypes.PNG,
    "txt" -> ContentTypes.TXT,
    "xml" -> ContentTypes.XML
  )

  def isValidContentType(t: String): Boolean = {
    (BaseFile.ContentTypes.binaryTypes ++ BaseFile.ContentTypes.textTypes).contains(t)
  }

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

case class CloneResourceResult(results: Seq[CloneFileResult])

trait CloneError {
  def message: String
}

case class MissingVersionFromId(vid: VersionedId[ObjectId]) extends CloneError {
  override def message = s"This id is missing a version: $vid"
}

case class CloningFailed(failures: Seq[CloneFileResult]) extends CloneError {
  override def message: String = s"Cloning failed for files: ${failures.map(_.file.name)}"
}

trait CloneFileResult {
  def file: StoredFile
  def successful: Boolean
}

trait CloneFileFailure extends CloneFileResult {
  final override def successful: Boolean = false
}

case class CloneFileSuccess(val file: StoredFile, s3Key: String) extends CloneFileResult {
  override def successful: Boolean = true
}

case class NotFoundCloneFileFailure(val file: StoredFile, val err: Throwable) extends CloneFileFailure
case class ErrorThrownCloneFileFailure(val file: StoredFile, val err: Throwable) extends CloneFileFailure
