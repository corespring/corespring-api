package scorm.utils

import java.io._
import scorm.models.extractors.RemoteItemRunner


object ScormExporter {

  val ScormFolder = "conf/scorm/item-scorm-2004"

  type NameContents = (String, String)

  def makeScormPackage(id: String, token: String): Array[Byte] = {
    Array()
  }

  def basename(n: String): String = {
    val parts = n.split("/")
    parts(parts.length - 1)
  }

  /** Write the scorm package to a zip
    */
  def makeScormPackageZip(id: String, token: String, folder: String = ".") {
    val scormFolderFiles = new File(ScormFolder).listFiles.toList

    val files = scormFolderFiles
      .filterNot(_.getName.endsWith(".template"))
      .map(ScormFolder + "/" + _.getName)

    def processTemplates(): List[Option[NameContents]] = {
      val templates = scormFolderFiles.filter(_.getName.endsWith("template"))
      templates.map {
        f: File => {
          (f, id) match {
            case RemoteItemRunner(n, c) => Some((n, c))
            case _ => None
          }
        }
      }
    }
    val templates = processTemplates()
    println(templates)
    zipToFile(folder + "/" + id + "-scorm-2004.zip", files, templates.flatten, basename)
  }

  def zipToFile(zipFilename: String, files: Iterable[String], stringFiles: List[NameContents] = List(), processName: (String => String) = (n => n)) {
    val arr = zip(files, stringFiles, processName)
    val filesOut = new FileOutputStream(zipFilename)
    filesOut.write(arr)
    filesOut.close()
  }

  /**
   * Create zip as a byte array from a list of files
   * @param files
   * @param processName - if you need to process the filename you can do so here
   * @return
   */
  def zip(files: Iterable[String], stringFiles: Iterable[(String, String)], processName: (String => String) = (n => n)): Array[Byte] = {
    import java.io.{BufferedInputStream, FileInputStream}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    val byteOutStream = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(byteOutStream)

    def streamIntoZip(name: String, in: InputStream) {
      zip.putNextEntry(new ZipEntry(name))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }

    def addStringFileToZip(name: String, contents: String) {

      println("addStringFileToZip: " + name)
      streamIntoZip(name, new ByteArrayInputStream(contents.getBytes))
    }

    def addFileToZip(name: String) {
      val processedName = processName(name)
      streamIntoZip(processedName, new BufferedInputStream(new FileInputStream(name)))
    }

    files.foreach(addFileToZip)

    println(">> stringFiles")
    println(stringFiles)

    stringFiles.foreach((p: (String, String)) => addStringFileToZip(p._1, p._2))

    zip.close()
    byteOutStream.toByteArray
  }

}
