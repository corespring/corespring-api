package tests.api.v1

import api.v1.ExporterApi
import java.io.{FileOutputStream, File}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.execute.Result
import org.specs2.mutable.BeforeAfter
import play.api.test.Helpers._
import org.corespring.platform.core.models.item.Item
import org.corespring.test.BaseTest
import org.corespring.platform.core.services.item.ItemServiceImpl

class ExporterApiTest extends BaseTest {

  sequential

  private def writeToFile(path:String, bytes:Array[Byte]){
    val file : File = new File(path)
    val output = new FileOutputStream(file)
    output.write(bytes)
    output.close()
  }

  private def unzip(path:String, folder:String){
    //import sys.process._
    //Seq("unzip", path, "-d", folder).!
    new UnzipUtil().unzip(path,folder)
  }

  "Exporter api" should {

    "export a scorm package" in new CleanBeforeAndAfter("exporter-api-test-data") {

      val id = "50098908e4b0f123a2d54c98"
      val result = ExporterApi.multiItemScorm2004(id)(fakeRequest())

      status(result) === OK

      val bytes = contentAsBytes(result)
      val path = base + "/file.zip"
      writeToFile(path, bytes)
      unzip(path, base)
      assertManifest( base + "/imsmanifest.xml", id)
    }
  }

  private def assertManifest(path:String, id : String) : Result = {
    val manifest = scala.xml.XML.loadFile(path)

    ItemServiceImpl.findOneById( VersionedId(new ObjectId(id)) ).map{
      i : Item =>
        val identifier = (manifest \ "resources" \ "resource" \ "@identifier").text
        identifier ===  i.id.toString()
    }.getOrElse(failure("couldn't find item"))
  }

  class CleanBeforeAndAfter(val base:String) extends BeforeAfter{

    import sys.process._

    def before{
      Seq("rm", "-fr", base).!
      Seq("mkdir", "-p", base).!
    }

    def after{
      Seq("rm", "-fr", base.split("/")(0)).!
    }
  }
}
