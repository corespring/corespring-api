package org.corespring.qtiToV2

import java.io.{FileWriter, File}

import scala.collection.Iterator
import scala.io.Source

/**
 * Caches the result of a Source's getLines into a temporary file buffer. Then provides interfaces for obtaining Source
 * objects from the cached file in future. File is removed when the JVM halts.
 */
class SourceWrapper(source: Source) {

  def prefix = s"source-wrapper-${source.hashCode}"
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
      val writer = new FileWriter(file)
      source.getLines.foreach(line => writer.write(s"$line\n"))
      writer.close()
      tempFile = Some(file)
      Source.fromFile(file)
    }
  }

  def mkString = getLines.mkString

  def map[B](f: Char => B): Iterator[B] = toSource.map[B](f)

  def getLines = toSource.getLines

}

object SourceWrapper {
  def apply(source: Source) = new SourceWrapper(source)
}