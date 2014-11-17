package org.corespring.qtiToV2

import java.io._

import org.apache.commons.io.FileUtils

import scala.collection.Iterator
import scala.io.Source

/**
 * Caches the result of a Source's getLines into a temporary file buffer. Then provides interfaces for obtaining Source
 * objects from the cached file in future. File is removed when the JVM halts.
 */
class SourceWrapper(inputStream: InputStream) {

  def prefix = s"source-wrapper-${inputStream.hashCode}"
  val suffix = ".tmp"

  var tempFile: Option[File] = None

  /**
   * Creates a Source object from the lines contained within the file. Provided for compatibility with APIs that require
   * a Source object.
   */
  def toSource = tempFile match {
    case Some(file) => Source.fromFile(file)
    case _ => {
      val file = File.createTempFile(prefix, suffix)
      file.deleteOnExit()
      FileUtils.copyInputStreamToFile(inputStream, file)
      tempFile = Some(file)
      Source.fromFile(file)("ISO-8859-1")
    }
  }

  def mkString = getLines.mkString

  def map[B](f: Char => B): Iterator[B] = toSource.map[B](f)

  def getLines = toSource.getLines

}

object SourceWrapper {
  def apply(inputStream: InputStream) = new SourceWrapper(inputStream)
}