package tests.scorm.utils

import org.specs2.mutable.{BeforeAfter, Specification}
import scorm.utils.ScormExporter
import java.io.{FileNotFoundException, File}
import org.bson.types.ObjectId
import tests.{BaseTest}
import tests.PlaySingleton
import models.item.Item

class ScormExporterTest extends BaseTest {

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


