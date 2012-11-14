package tests.scorm.utils

import org.specs2.mutable.{BeforeAfter, Specification}
import scorm.utils.ScormExporter
import java.io.{FileNotFoundException, File}

class ScormExporterTest extends Specification {

  val MockZipFolder = "test/mockZip"

  def mockFolder(s: String) = MockZipFolder + s

  "Scorm exporter" should {
    "export to file " in new fileTidier {

      def zipFolder = MockZipFolder

      val files: List[String] = List("test/mockXml/all-items.xml")
      ScormExporter.zipToFile(mockFolder("/test.zip"), files)
      val zipFile = new File(mockFolder("/test.zip"))
      val zipExists = zipFile.exists()
      zipExists === true
    }

    "export scorm package" in new fileTidier {
      def zipFolder = "test/mockScormZips"
      ScormExporter.makeScormPackageZip("12345", "12345", zipFolder)
      new File( zipFolder + "/12345-scorm-2004.zip").exists() === true
    }
  }
}

trait fileTidier extends BeforeAfter {

  /** Define in instance
    */
  def zipFolder: String

  def before = new File(zipFolder).mkdir

  def after {
    delete(new File(zipFolder))
  }

  def delete(f: File) {
    try {
      if (f.isDirectory) {
        for (c <- f.listFiles())
          delete(c)
      }
      if (!f.delete())
        throw new FileNotFoundException("Failed to delete file: " + f)
    }
    catch {
      case e: Throwable => println("error deleting folder")
    }
  }
}


