package org.corespring.qtiToV2

import scala.collection.Iterator
import scala.io.Source

/**
 * Caches the result of a Source's getLines, since the underlying InputStream of a Source can sometimes only be read
 * once. Use with caution, as this is infeasible for large Source objects. Current use case is for ~20kb XML files, so
 * it is currently suitable.
 */
class SourceWrapper(source: Source) {

  var lines: Option[Seq[String]] = None

  def getLines = lines match {
    case Some(lines) => lines.toIterator
    case _ => {
      lines = Some(source.getLines.toSeq)
      lines.get.toIterator
    }
  }

  def mkString = getLines.mkString

  def map[B](f: Char => B): Iterator[B] = source.map[B](f)

}

object SourceWrapper {
  def apply(source: Source) = new SourceWrapper(source)
}