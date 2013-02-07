package tests.api.v1

import api.ApiError
import models._
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.Play
import play.api.Play.current

import play.api.test.Helpers._
import scala._
import play.api.test.FakeHeaders
import scala.Some
import play.api.mvc.SimpleResult
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import tests.BaseTest

class ResourceApiTest extends BaseTest {


  def testItemId : String = testItem.id.toString

  val testRoutes : api.v1.ReverseResourceApi = api.v1.routes.ResourceApi

  def call(url : String, method : String, byteArray: Array[Byte], expectedStatus: Int, expectedContains: String): (Boolean, Boolean) = {

    routeAndCall(
      tokenFakeRequest(
        method,
        url,
        FakeHeaders(Map("Content" -> List("application/octet-stream"))),
        byteArray)) match {
      case Some(result) => {
        val simpleResult = result.asInstanceOf[SimpleResult[JsValue]]
        val stringResult = contentAsString(simpleResult)
        (status(result) == expectedStatus, stringResult.contains(expectedContains))
      }
      case _ => {
        throw new RuntimeException("Error with call")
      }
    }
  }

  def testItem: Item = item("511156d38604c9f77da9739d")

  def rubric: Resource = {
    testItem.supportingMaterials.find(_.name == "Rubric") match {
      case Some(r) => r
      case _ => throw new RuntimeException("can't find rubric")
    }
  }

  "resource api" should {

    def makeFileRequest(file: VirtualFile, path: String, method: String = POST): Result = {

      val request = tokenFakeRequest(method, path, FakeHeaders(), AnyContentAsJson(Json.toJson(file)))

      routeAndCall(request) match {
        case Some(result) => {
          result
        }
        case _ => {
          throw new RuntimeException("Request failed")
        }
      }
    }

    "delete a file from the Item.data Resource" in {
      val create = testRoutes.createDataFile(testItemId)
      val delete =  testRoutes.deleteDataFile(testItemId, "myfile.txt")
      val file = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")
      assertDelete(create, delete, testItem.data.get, file)
    }

    "delete a file from a supportingMaterial Resource" in {
      val create = testRoutes.createSupportingMaterialFile(testItemId, "Rubric")
      val delete = testRoutes.deleteSupportingMaterialFile(testItemId, "Rubric", "myfile.txt")
      val file = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")
      assertDelete(create, delete, rubric, file)
    }

    def assertDelete(create : Call, delete : Call, resource : => Resource, file : VirtualFile) = {
      val initialLength = resource.files.length
      makeFileRequest(file, create.url, create.method)
      val length = resource.files.length
      length must equalTo(initialLength + 1)
      makeFileRequest(file, delete.url, delete.method)
      resource.files.length must equalTo(initialLength)
    }

    "creating or updating a file to default in Item.data is ignored" in {

      val create = testRoutes.createDataFile(testItemId)
      val f0 = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")

      makeFileRequest(f0, create.url, create.method)

      testItem.data.get.files.find(_.name == f0.name) match {
        case Some(f) => {
          f.isMain must equalTo(false)
        }
        case _ => failure("Can't find new file")
      }

      val update = testRoutes.updateDataFile(testItemId, "myfile.txt")
      makeFileRequest(f0, update.url, update.method)

      testItem.data.get.files.find(_.name == f0.name) match {
        case Some(f) => {
          f.isMain must equalTo(false)
        }
        case _ => failure("Can't find new file")
      }
    }


    "update a file in supporting materials" in {
      val create = testRoutes.createSupportingMaterialFile(testItemId, "Rubric")
      val file = VirtualFile("data.txt", "text/txt", isMain = false, content = "f0")
      val update = testRoutes.updateSupportingMaterialFile(testItemId, "Rubric", "data.txt")
      assertUpdate(create, update, file, ( _ => testItem.supportingMaterials.find(_.name == "Rubric").get))
    }

    "update a file in Item.data" in {

      val create = testRoutes.createDataFile(testItemId)
      val file = VirtualFile("data.txt", "text/txt", isMain = false, content = "f0")
      val update = testRoutes.updateDataFile(testItemId, "data.txt")
      assertUpdate(create, update, file, ( _ => testItem.data.get))
    }

    def assertUpdate(create:Call,update:Call, file: VirtualFile, resourceFn : ( Unit => Resource)) = {

      makeFileRequest(file, create.url, create.method)

      val updateFile = VirtualFile("newName2.txt", "text/txt", isMain = false, content = "new content")
      val result = makeFileRequest(updateFile, update.url, update.method)
      status(result) must equalTo(OK)
      val item = testItem

      resourceFn().files.find(_.name == updateFile.name) match {
        case Some(f) => {
          f.asInstanceOf[VirtualFile].content must equalTo(updateFile.content)
        }
        case _ => failure("can't find updated file")
      }
    }


    "when creating a file - if its default - unsets the other items in the file list" in {

      val create = testRoutes.createSupportingMaterialFile(testItemId, "Rubric")

      val f0 = VirtualFile("data.file.0.default.txt", "text/txt", isMain = true, content = "f0")
      val f1 = VirtualFile("data.file.default.txt", "text/txt", isMain = true, content = "hello there")

      makeFileRequest(f0, create.url, create.method)
      val result = makeFileRequest(f1, create.url, create.method)

      val json = Json.parse(contentAsString(result))
      json.as[BaseFile].name must equalTo("data.file.default.txt")
      json.as[BaseFile].isMain must equalTo(true)

      status(result) must equalTo(OK)

      val item: Item = Item.findOneById(testItem.id).get

      item.supportingMaterials.find(_.name == "Rubric") match {
        case Some(r) => {
          r.files.find(_.isMain == true) match {
            case Some(defaultFile) => {
              defaultFile.name must equalTo(f1.name)
            }
            case None => failure("couldn't find default file")
          }
        }
        case _ => failure("can't find resource")
      }
    }

    "create a virtual file in Item.data resource" in {
      val create = testRoutes.createDataFile(testItemId)
      assertCantCreateTwoFilesWithSameName(create)
    }

    "create a virtual file in a supporting material resource" in {
      val create = testRoutes.createSupportingMaterialFile(testItemId, "Rubric")
      assertCantCreateTwoFilesWithSameName(create)
    }

    def assertCantCreateTwoFilesWithSameName(create:Call) = {
      val f = VirtualFile("data.file.txt", "text/txt", isMain = false, content = "hello there")
      val result = makeFileRequest(f, create.url, create.method)
      val json = Json.parse(contentAsString(result))
      json.as[BaseFile].name must equalTo("data.file.txt")
      status(result) must equalTo(OK)
      val secondResult = makeFileRequest(f, create.url, create.method)
      status(secondResult) must equalTo(NOT_ACCEPTABLE)
    }

    "create a new supporting material resource" in {

      val create : Call = testRoutes.createSupportingMaterial(testItemId)
      val request = tokenFakeRequest(create.method, create.url, FakeHeaders())
      routeAndCall(request) match {
        case Some(result) => {
          status(result) must equalTo(BAD_REQUEST)
        }
        case _ => failure("Request failed")
      }

      val r: Resource = Resource("newResource", Seq())

      routeAndCall(tokenFakeRequest(create.method, create.url, FakeHeaders(), AnyContentAsJson(Json.toJson(r)))) match {
        case Some(result) => {
          status(result) must equalTo(OK)
        }
        case _ => {
          throw new RuntimeException("Request failed")
        }
      }

      routeAndCall(tokenFakeRequest(create.method, create.url, FakeHeaders(), AnyContentAsJson(Json.toJson(r)))) match {
        case Some(result) => {
          contentAsString(result).contains(ApiError.ResourceNameTaken.message) must equalTo(true)
          status(result) must equalTo(NOT_ACCEPTABLE)
        }
        case _ => {
          throw new RuntimeException("Request failed")
        }
      }

      val delete = testRoutes.deleteSupportingMaterial(testItemId, "newResource")
      //tidy up
      routeAndCall(tokenFakeRequest(delete.method, delete.url)) match {
        case Some(result) => status(result) must equalTo(OK)
        case _ => failure("delete failed")
      }
    }

    "delete a new supporting material resource" in {

      val resourceName = "newResource2"
      val delete = testRoutes.deleteSupportingMaterial(testItemId, resourceName)
      val create = testRoutes.createSupportingMaterial(testItemId)

      val r: Resource = Resource("newResource2", Seq())

      routeAndCall(tokenFakeRequest(create.method, create.url, FakeHeaders(), AnyContentAsJson(Json.toJson(r))))
      routeAndCall(tokenFakeRequest(delete.method, delete.url)) match {
        case Some(result) => {
          status(result) must equalTo(OK)
        }
        case _ => throw new RuntimeException("Request Failed")
      }

    }


    "list an item's supporting materials" in {

      val get = testRoutes.getSupportingMaterials(testItemId)
      routeAndCall(tokenFakeRequest(get.method, get.url)) match {
        case Some(result) => {
          val json: JsValue = Json.parse(contentAsString(result.asInstanceOf[SimpleResult[JsValue]]))
          val seq: Seq[JsObject] = json.as[Seq[JsObject]]
          seq.length must equalTo(1)
          val jsItem = seq(0)
          (jsItem \ "name").asOpt[String] must equalTo(Some("Rubric"))
        }
        case _ => throw new RuntimeException("call failed")
      }
    }

    "do a binary post to supporting materials Resource" in {

      val item = testItem
      val filename = "cute-rabbit.jpg"
      val create = testRoutes.uploadFile(item.id.toString,"Rubric", filename)
      val file = Play.getFile("test/tests/files/" + filename)
      val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()

      //First upload should work
      val firstCall = call(create.url, create.method, byteArray,  OK, filename)
      firstCall must equalTo((true, true))
      //Second call is not acceptable - the file is already existing
      val secondCall = call(create.url, create.method, byteArray, NOT_FOUND, ApiError.FilenameTaken.message)
      secondCall must equalTo((true, true))

      val badUpdate = testRoutes.uploadFile(item.id.toString, "badResourceName", filename)
      val thirdCall = call(badUpdate.url, badUpdate.method, byteArray,  NOT_FOUND, ApiError.ResourceNotFound.message)
      thirdCall must equalTo((true, true))
    }

    "do a binary post to Item.data resource" in {

      val item = testItem
      val filename = "cute-rabbit.jpg"

      val update = testRoutes.uploadFileToData(item.id.toString, filename)
      val file = Play.getFile("test/tests/files/" + filename)
      val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()

      //First upload should work
      val firstCall = call(update.url, update.method, byteArray, OK, filename)
      firstCall must equalTo((true, true))

      val secondCall = call(update.url, update.method, byteArray, NOT_FOUND, ApiError.FilenameTaken.message)
      secondCall must equalTo((true, true))
    }
  }
}

