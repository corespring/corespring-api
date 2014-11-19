package org.corespring.qtiToV2

import java.io._
import java.nio.charset.{MalformedInputException, CodingErrorAction}

import org.apache.commons.io.FileUtils

import scala.collection.Iterator
import scala.io.{Codec, Source}

/**
 * Caches the result of a Source's getLines into a temporary file buffer. Then provides interfaces for obtaining Source
 * objects from the cached file in future. File is removed when the JVM halts.
 */
class SourceWrapper(inputStream: InputStream) {

  def prefix = s"source-wrapper-${inputStream.hashCode}"
  val suffix = ".tmp"

  var tempFile: Option[File] = None

  private def getFile: File = tempFile match {
    case Some(file) => file
    case _ => {
      val file = File.createTempFile(prefix, suffix)
      file.deleteOnExit()
      FileUtils.copyInputStreamToFile(inputStream, file)
      tempFile = Some(file)
      file
    }
  }

  /**
   * Creates a Source object from the lines contained within the file. Provided for compatibility with APIs that require
   * a Source object.
   */
  def toSource = {
    val codec = Codec("ISO-8859-1")
    codec.onMalformedInput(CodingErrorAction.IGNORE)
    codec.onUnmappableCharacter(CodingErrorAction.IGNORE)
      Source.fromFile(getFile)(codec)
  }

  def mkString = getLines.mkString

  def toByteArray: Array[Byte] = {
    val input = new FileInputStream(getFile)
    val output = new ByteArrayOutputStream()
    var buffer = new Array[Byte](65536)
    var l = input.read(buffer)
    while (l > 0) {
      output.write (buffer, 0, l)
      l = input.read(buffer)
    }
    input.close()
    output.close()
    output.toByteArray()
  }

  def getLines = toSource.getLines

}

object SourceWrapper {
  def apply(inputStream: InputStream) = new SourceWrapper(inputStream)
}