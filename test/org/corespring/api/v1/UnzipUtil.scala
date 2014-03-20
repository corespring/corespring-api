package org.corespring.api.v1

import java.io._
import java.util.jar._
import play.api.Logger

class UnzipUtil {

  val logger = Logger("UnzipUtil")

  def externalUnzip(path: String, folder: String) {
    import sys.process._
    Seq("unzip", path, "-d", folder).!
  }

  def unzip(filePath: String, toDir: String): Unit = {
    unzip(new File(filePath), new File(toDir))
  }

  def unzip(file: File, todir: File): Unit = {
    todir.mkdirs()

    logger.info(s"Extracting $file to $todir")
    val jar = new JarFile(file)
    val enu = jar.entries
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      val entryPath = entry.getName

      logger.info(s"Extracting to $todir/$entryPath")
      if (entry.isDirectory) {
        new File(todir, entryPath).mkdirs
      } else {
        val istream = jar.getInputStream(entry)
        val ostream = new FileOutputStream(new File(todir, entryPath))
        copyStream(istream, ostream)
        ostream.close
        istream.close
      }
    }
  }

  private def copyStream(istream: InputStream, ostream: OutputStream): Unit = {
    var bytes = new Array[Byte](1024)
    var len = -1
    while ({
      len = istream.read(bytes, 0, 1024);
      len != -1
    }) {
      ostream.write(bytes, 0, len)
    }
  }

}
