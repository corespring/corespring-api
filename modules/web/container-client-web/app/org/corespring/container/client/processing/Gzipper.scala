package org.corespring.container.client.processing

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

trait Gzipper {
  def gzip(src: String): Array[Byte] = {
    val out: ByteArrayOutputStream = new ByteArrayOutputStream()
    val gzip: GZIPOutputStream = new GZIPOutputStream(out)
    gzip.write(src.getBytes())
    gzip.close()
    val returnValue = out.toByteArray
    out.close()
    returnValue
  }
}
