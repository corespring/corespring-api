package scorm.utils

import java.io._
import scorm.models.extractors.RemoteItemRunnerTemplate
import models.Item
import scorm.models.Builder
import scorm.models.Builder.Config


object ScormExporter {

  val ScormFolder = "conf/scorm/multi-item-scorm-2004"
  val ScormFolderFiles = new File(ScormFolder).listFiles.toList
  val PlainFiles = ScormFolderFiles.filterNot(_.getName.endsWith(".template"))
  val TemplateFiles = ScormFolderFiles.filter(_.getName.endsWith(".template"))

  type NameContents = (String, String)

  def makeMultiScormPackage(items: List[Item], token: String, host: String): Array[Byte] = {
    val plainFilePaths = PlainFiles.map(ScormFolder + "/" + _.getName)
    val tokens: Map[String, String] = Map("corespringDomain" -> host)
    val processedTemplates = processTemplates(tokens)
    val config: Config = Config(host, common.mock.MockToken)
    val manifest = Builder.Manifest(items, config)
    val stringFiles = (("imsmanifest.xml", manifest.mkString("\n"))) :: processedTemplates.flatten
    zip(plainFilePaths, stringFiles, basename)
  }

  private def processTemplates(tokens: Map[String, String]): List[Option[NameContents]] = {
    TemplateFiles.map {
      f: File => {
        (f, tokens) match {
          case RemoteItemRunnerTemplate(n, c) => Some((n, c))
          case _ => None
        }
      }
    }
  }


  def basename(n: String): String = {
    val parts = n.split("/")
    parts(parts.length - 1)
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
      streamIntoZip(name, new ByteArrayInputStream(contents.getBytes))
    }

    def addFileToZip(name: String) {
      val processedName = processName(name)
      streamIntoZip(processedName, new BufferedInputStream(new FileInputStream(name)))
    }

    files.foreach(addFileToZip)
    stringFiles.foreach((p: (String, String)) => addStringFileToZip(p._1, p._2))

    zip.close()
    byteOutStream.toByteArray
  }

}
