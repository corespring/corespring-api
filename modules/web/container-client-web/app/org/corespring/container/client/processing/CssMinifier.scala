package org.corespring.container.client.processing

import java.io.{ StringReader, StringWriter }

trait CssMinifier {

  def minifyCss(contents: String): Either[String, String] = {
    val reader = new StringReader(contents)
    val writer = new StringWriter()
    try {
      val compressor = new com.yahoo.platform.yui.compressor.CssCompressor(reader)
      reader.close()
      compressor.compress(writer, 0)
      val out = writer.toString
      writer.close()
      Right(out)
    } catch {
      case e: Throwable => Left(e.getMessage)
    }
  }
}
