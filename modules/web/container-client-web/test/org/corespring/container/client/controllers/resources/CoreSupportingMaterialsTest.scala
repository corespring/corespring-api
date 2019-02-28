package org.corespring.container.client.controllers.resources

import java.io.{ ByteArrayInputStream, BufferedInputStream, FileInputStream, File }
import java.nio.charset.Charset

import org.apache.commons.io.{ Charsets, IOUtils }
import org.corespring.container.client.controllers.resources.CoreSupportingMaterials.Errors
import org.corespring.container.client.hooks._
import org.corespring.test.TestContext
import org.mockito.Matchers.{ eq => e }
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.Files
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.test.{ FakeHeaders, PlaySpecification, FakeRequest }

import scala.concurrent.{ Future, ExecutionContext }
import scalaz.Success

class CoreSupportingMaterialsTest extends Specification with Mockito with PlaySpecification {

  lazy val testFile = {
    val r = this.getClass.getResource("/image.png")
    if (r == null) {
      throw new RuntimeException("can't find resource for: /image.png")
    }
    val f = new File(r.toURI())
    if (!f.exists) {
      throw new RuntimeException("can't find /image.png")
    }
    f
  }

  lazy val testFileBytes = {
    val stream = new FileInputStream(testFile)
    val bytes = IOUtils.toByteArray(stream)
    IOUtils.closeQuietly(stream)
    bytes
  }

  class testScope extends Scope with CoreSupportingMaterials with TestContext {

    override def getTimestamp = "stamp"

    val rh = FakeRequest()

    lazy val mockHooks = {
      val m = mock[SupportingMaterialHooks]
      m.create(anyString, any[CreateBinaryMaterial])(any[Request[AnyContent]]) returns Future { Right(Json.obj()) }
      m
    }
    override def materialHooks: SupportingMaterialHooks = mockHooks
  }

  case class beError(e: CoreSupportingMaterials.Error) extends Matcher[Future[SimpleResult]] {
    def apply[S <: Future[SimpleResult]](s: Expectable[S]) = {

      val actualStatus = status(s.value)
      val actualContent = contentAsJson(s.value)
      val statusOk = actualStatus == e.code
      val contentOk = actualContent == e.json

      lazy val errorMsg: String = {
        Seq(
          if (!statusOk) Some(s"status of result (${actualStatus}) !== expected status (${e.code})") else None,
          if (!contentOk) Some(s"content of result (${actualContent}) !== expected  (${e.json})") else None).flatten.mkString(",")
      }

      result(statusOk && contentOk,
        s"result maps to error: ${e.json}",
        errorMsg,
        s)
    }
  }

  def mkForm(dataParts: Map[String, Seq[String]] = Map.empty,
    files: Seq[MultipartFormData.FilePart[Files.TemporaryFile]] = Seq.empty) = {
    MultipartFormData[Files.TemporaryFile](dataParts, files, badParts = Seq.empty, missingFileParts = Seq.empty)
  }

  def mkFormWithFile(params: Map[String, String], filename: String = "image.png", file: File = testFile, contentType: Option[String] = None) = {
    val files = Seq(FilePart[Files.TemporaryFile]("file", filename, contentType, Files.TemporaryFile(file)))
    val dataParts = params.mapValues(Seq(_))
    mkForm(dataParts, files)
  }

  def req(form: MultipartFormData[Files.TemporaryFile]) = {
    FakeRequest("", "", FakeHeaders(), AnyContentAsMultipartFormData(form))
  }

  "createSupportingMaterial" should {

    class createJson extends testScope {
      def req(json: JsValue) = {
        FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json))
      }
    }

    "fail if request body is not json" in new createJson {
      createSupportingMaterial("id")(rh) must beError(Errors.notJson)
    }

    "fail if there is no name in the json" in new createJson {
      createSupportingMaterial("id")(req(Json.obj())) must beError(Errors.cantFindParameter("name"))
    }

    "fail if there is no materialType in the json" in new createJson {
      createSupportingMaterial("id")(req(Json.obj("name" -> "name"))) must beError(Errors.cantFindParameter("materialType"))
    }

    "fail if there is no html in the json" in new createJson {
      createSupportingMaterial("id")(req(Json.obj("name" -> "name", "materialType" -> "type"))) must beError(Errors.cantFindParameter("html"))
    }

    "call hooks.create" in new createJson {
      val request = req(Json.obj("name" -> "name", "materialType" -> "type", "html" -> "<div>hi</div>"))
      createSupportingMaterial("id")(request)
      val captor = capture[CreateHtmlMaterial]
      there was one(materialHooks).create(e("id"), captor)(any[RequestHeader])
      captor.value.name === "name"
      captor.value.materialType === "type"
      captor.value.main === Html("index.html", "<div>hi</div>")
    }
  }

  "createSupportingMaterialFromFile" should {

    class create extends testScope

    "fail if there is no MultiPart form data in the request body" in new create {
      val result = createSupportingMaterialFromFile("id")(FakeRequest("", ""))
      result must beError(Errors.notMultipartForm)
    }

    "fail if there is no param 'file' in the form" in new create {
      val data = mkForm()
      val result = createSupportingMaterialFromFile("id")(req(data))
      result must beError(Errors.cantFindParameter("file"))
    }

    "fail if there is no param 'materialType' in the form" in new create {
      val data = mkFormWithFile(Map.empty)
      val result = createSupportingMaterialFromFile("id")(req(data))
      result must beError(Errors.cantFindParameter("materialType"))
    }

    "fail if the contentType can't be found for 'file'" in new create {
      val data = mkFormWithFile(Map("materialType" -> "Rubric"), "image.unknown_type")
      val result = createSupportingMaterialFromFile("id")(req(data))
      result must beError(Errors.cantDetectMimeType("image.unknown_type", None))
    }

    "fail if the contentType is not acceptable" in new create {
      val data = mkFormWithFile(Map("materialType" -> "Rubric"), "doc.doc", contentType = Some("application/msword"))
      val result = createSupportingMaterialFromFile("id")(req(data))
      result must beError(Errors.mimeTypeNotAcceptable("application/msword", acceptableTypes))
    }

    "uses the file name if there is no 'name' in the form" in new create {
      val data = mkFormWithFile(Map("materialType" -> "Rubric"))
      val request = req(data)
      createSupportingMaterialFromFile("id")(request)
      val captor = capture[CreateBinaryMaterial]
      there was one(materialHooks).create(e("id"), captor)(any[RequestHeader])
      captor.value.name === "image.png"
    }

    "uses the 'name' parameter in the form" in new create {
      val data = mkFormWithFile(Map("materialType" -> "Rubric", "name" -> "my-material"))
      val request = req(data)
      createSupportingMaterialFromFile("id")(request)
      val captor = capture[CreateBinaryMaterial]
      there was one(materialHooks).create(e("id"), captor)(any[RequestHeader])
      captor.value.name === "my-material"
    }

    "return error from hooks.create" in new create {
      mockHooks.create(any[String], any[CreateBinaryMaterial])(any[RequestHeader]) returns Future(Left(1, "error"))
      val data = mkFormWithFile(Map("materialType" -> "Rubric"))
      val request = req(data)
      val result = createSupportingMaterialFromFile("id")(request)
      status(result) === 1
      contentAsString(result) === "error"
    }
  }

  "deleteSupportingMaterial" should {

    class delete extends testScope {
      mockHooks.delete(any[String], any[String])(any[RequestHeader]) returns Future(Right(Json.obj()))
    }

    "call hooks.delete" in new delete {
      deleteSupportingMaterial("id", "name")(rh)
      there was one(materialHooks).delete("id", "name")(rh)
    }
  }

  "updateSupportingMaterialContent" should {

    class update extends testScope {
      mockHooks.updateContent(any[String], any[String], any[String], any[String])(any[RequestHeader]) returns Future.successful(Right(Json.obj()))
    }

    "fail if the body isn't text" in new update {
      updateSupportingMaterialContent("id", "materialName", "filename")(rh) must beError(Errors.notText)
    }

    "call hooks.updateContent" in new update {
      val request = FakeRequest("", "", FakeHeaders(), AnyContentAsText("hi"))
      updateSupportingMaterialContent("id", "materialName", "filename")(request)
      there was one(materialHooks).updateContent("id", "materialName", "filename", "hi")(request)
    }

    "returns hooks error" in new update {
      mockHooks.updateContent(any[String], any[String], any[String], any[String])(any[RequestHeader]) returns Future.successful(Left(1, "error"))
      val result = updateSupportingMaterialContent("id", "materialName", "filename")(FakeRequest("", "", FakeHeaders(), AnyContentAsText("hi"))) //must beError(Errors.notText)
      println(contentAsString(result))
      status(result) === 1
      contentAsString(result) === "error"
    }
  }

  "addAssetToSupportingMaterialContent" should {

    class addAsset extends testScope {
      mockHooks.addAsset(any[String], any[String], any[Binary])(any[RequestHeader]) returns Future.successful(Right(UploadResult("path.png")))
    }

    "fail if the body isn't multipart form data" in new addAsset {
      addAssetToSupportingMaterial("id", "name")(rh) must beError(Errors.notMultipartForm)
    }

    "fail if the mimeType is pdf" in new addAsset {
      val form = mkFormWithFile(Map.empty, contentType = Some("application/pdf"))
      val request = req(form)
      addAssetToSupportingMaterial("id", "name")(request) must
        beError(Errors.mimeTypeNotAcceptable("application/pdf", acceptableTypes.filterNot(_ == "application/pdf")))
    }

    //    "call hooks.addAsset" in new addAsset {
    //      val form = mkFormWithFile(Map.empty)
    //      val request = req(form)
    //      val result = addAssetToSupportingMaterial("id", "name")(request)
    //      val captor = capture[Binary]
    //      status(result) === OK
    //      there was one(materialHooks).addAsset(e("id"), e("name"), captor)(any[RequestHeader])
    //      captor.value.name === "stamp-image.png"
    //      captor.value.mimeType === "image/png"
    //    }.pendingUntilFixed
    //
    //    "return hooks errors" in new addAsset {
    //      mockHooks.addAsset(any[String], any[String], any[Binary])(any[RequestHeader]) returns Future.successful(Left(1 -> "error"))
    //      val form = mkFormWithFile(Map.empty)
    //      val request = req(form)
    //      val result = addAssetToSupportingMaterial("id", "name")(request)
    //      status(result) === 1
    //      contentAsString(result) === "error"
    //    }.pendingUntilFixed
  }

  "deleteAssetFromSupportingMaterial" should {
    class deleteAsset extends testScope {
      mockHooks.deleteAsset(any[String], any[String], any[String])(any[RequestHeader]) returns Future.successful(Right(Json.obj()))
    }

    "call hooks.deleteAsset" in new deleteAsset {
      deleteAssetFromSupportingMaterial("id", "name", "filename")(rh)
      there was one(materialHooks).deleteAsset("id", "name", "filename")(rh)
    }
  }

  "getAssetFromSupportingMaterial" should {
    class getAsset extends testScope {
      val fd = FileDataStream(new ByteArrayInputStream("".getBytes(Charsets.UTF_8)), 0, "none", Map.empty)
      mockHooks.getAsset(any[String], any[String], any[String])(any[RequestHeader]) returns Future.successful(Right(fd))
    }

    class getAssetOctetStream extends testScope {
      val fd = FileDataStream(new ByteArrayInputStream("".getBytes(Charsets.UTF_8)), 0, "application/octet-stream", Map.empty)
      mockHooks.getAsset(any[String], any[String], any[String])(any[RequestHeader]) returns Future.successful(Right(fd))
    }

    class getAssetImage extends testScope {
      val fd = FileDataStream(new ByteArrayInputStream("".getBytes(Charsets.UTF_8)), 0, "image/png", Map.empty)
      mockHooks.getAsset(any[String], any[String], any[String])(any[RequestHeader]) returns Future.successful(Right(fd))
    }

    "call hooks.getAsset" in new getAsset {
      val result = getAssetFromSupportingMaterial("id", "name", "filename")(rh)
      status(result) === OK
      there was one(materialHooks).getAsset("id", "name", "filename")(rh)
    }

    "octet stream content type will not be used in the header" in new getAssetOctetStream {
      val result = getAssetFromSupportingMaterial("id", "name", "filename")(rh)
      contentType(result) === None
    }

    "image content type will be used in the header" in new getAssetImage {
      val result = getAssetFromSupportingMaterial("id", "name", "filename")(rh)
      contentType(result) === Some("image/png")
    }
  }
}
