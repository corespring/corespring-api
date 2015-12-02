package org.corespring.v2.player.supportingMaterials

import java.io.File

import org.corespring.it.MultipartFormDataWriteable
import org.corespring.it.assets.ImageUtils
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.libs.{ Files, MimeTypes }
import play.api.mvc._
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.{ PlaySpecification, RouteInvokers }

import scala.concurrent.Future

private[supportingMaterials] trait testDefaults {

  val materialName = "html-material"

  val json = Json.obj(
    "name" -> materialName,
    "materialType" -> "Rubric",
    "html" -> "<div>Hi</div>")
}

trait addFileScope extends withUploadFile with Helpers.requestToFuture {

  import MultipartFormDataWriteable.writeableOf_multipartFormData

  def filePath: String = s"/test-images/puppy.small.jpg"

  def addFileCall: Call
  def makeFormRequest(call: Call, form: MultipartFormData[Files.TemporaryFile]): Request[AnyContentAsMultipartFormData]

  def addFile = {
    val form = mkFormWithFile(Map.empty)
    val req = makeFormRequest(addFileCall, form)
    futureResult(req)
  }
}

object Helpers extends PlaySpecification {
  trait requestToFuture {

    protected def futureResult[T](r: Request[T])(implicit w: Writeable[T]): Future[SimpleResult] = {
      route(r).getOrElse {
        throw new RuntimeException(s"route for request: $r failed")
      }
    }
  }
}

trait MultipartForms {

  def filename:String
  def contentType:String
  def file:File

  def mkForm(dataParts: Map[String, Seq[String]] = Map.empty,
             files: Seq[MultipartFormData.FilePart[Files.TemporaryFile]] = Seq.empty) = {
    MultipartFormData[Files.TemporaryFile](dataParts, files, badParts = Seq.empty, missingFileParts = Seq.empty)
  }

  def mkFormWithFile(params: Map[String, String]) = {
    val files = Seq(FilePart[Files.TemporaryFile]("file", filename, Some(contentType), Files.TemporaryFile(file)))
    val dataParts = params.mapValues(Seq(_))
    mkForm(dataParts, files)
  }

}

trait withUploadFile extends MultipartForms {

  def filePath: String

  /**
   * Note - we need to create a temporary file as it is going to be deleted as part of the multipart upload.
   */
  lazy val (file, filename, contentType) = {
    import grizzled.file.GrizzledFile._
    import grizzled.file.util
    val f = ImageUtils.resourcePathToFile(filePath)
    val (dir, basename, ext) = f.dirnameBasenameExtension
    val filename = s"$basename$ext"
    val dest: File = f.copyTo(util.joinPath(dir.getAbsolutePath, s"$basename.tmp$ext"))
    val contentType = MimeTypes.forFileName(filename).getOrElse {
      throw new RuntimeException(s"Can't decide on mimeType for $filename")
    }

    (dest, filename, contentType)
  }


  def fileCleanUp = {
    println(s"deleting file: ${file.getAbsolutePath}")
    file.delete()
  }
}
