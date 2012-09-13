import api.ApiError
import api.v1.ResourceApi
import com.mongodb.{BasicDBObject, DBObject}
import models.{BaseFile, VirtualFile, Resource, Item}
import org.bson.types.ObjectId
import play.api.libs.json.{JsString, JsObject, Json, JsValue}
import play.api.mvc.{Result, AnyContentAsEmpty, AnyContentAsJson, SimpleResult}
import play.api.Play
import play.api.Play.current
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class ResourceApiTest extends BaseTest {

  val ACCESS_TOKEN: String = Global.MOCK_ACCESS_TOKEN

  def tokenize(url: String): String = url + "?access_token=" + ACCESS_TOKEN


  def tokenRequest[A](method: String, uri: String, headers: FakeHeaders = FakeHeaders(), body: A = ""): FakeRequest[A] = {
    FakeRequest(method, tokenize(uri), headers, body)
  }

  def call(byteArray: Array[Byte], url: String, expectedStatus: Int, expectedContains: String): (Boolean, Boolean) = {

    routeAndCall(
      tokenRequest(
        POST,
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

  def baseItemPath(itemId: String) = {
    "/api/v1/items/" + itemId
  }

  def testItem: Item = {

    val query: DBObject = new BasicDBObject()
    Item.findOneById(new ObjectId("50083ba9e4b071cb5ef79101")) match {
      case Some(item) => {
        item
      }
      case _ => throw new RuntimeException("test item")
    }
  }

  def rubric: Resource = {
    testItem.supportingMaterials.find(_.name == "Rubric") match {
      case Some(r) => r
      case _ => throw new RuntimeException("can't find rubric")
    }
  }

  "resource api" should {


    def makeFileRequest(file: VirtualFile, path: String, method: String = POST): Result = {

      val request = tokenRequest(method, path, FakeHeaders(), AnyContentAsJson(Json.toJson(file)))

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
      val url = baseItemPath(testItem.id.toString) + "/data"

      val f0 = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")

      val initialLength = testItem.data.get.files.length

      makeFileRequest(f0, url)

      val length = testItem.data.get.files.length

      length must equalTo(initialLength + 1)

      makeFileRequest(f0, url + "/myfile.txt", DELETE)

      testItem.data.get.files.length must equalTo(initialLength)
    }


    "delete a file from a supportingMaterial Resource" in {
      val url = baseItemPath(testItem.id.toString) + "/materials/Rubric"

      val f0 = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")

      val initialLength = rubric.files.length

      makeFileRequest(f0, url)

      val length = rubric.files.length

      length must equalTo(initialLength + 1)

      makeFileRequest(f0, url + "/myfile.txt", DELETE)
      rubric.files.length must equalTo(initialLength)
    }


    "creating or updating a file to default in Item.data is ignored" in {

      val url = baseItemPath(testItem.id.toString) + "/data"

      val f0 = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")

      makeFileRequest(f0, url)

      testItem.data.get.files.find(_.name == f0.name) match {
        case Some(f) => {
          f.isMain must equalTo(false)
        }
        case _ => failure("Can't find new file")
      }

      makeFileRequest(f0, url + "/myfile.txt", PUT)

      testItem.data.get.files.find(_.name == f0.name) match {
        case Some(f) => {
          f.isMain must equalTo(false)
        }
        case _ => failure("Can't find new file")
      }
    }

    "update a file in supporting materials" in {
      val url = baseItemPath(testItem.id.toString) + "/materials/Rubric"

      val f0 = VirtualFile("data.txt", "text/txt", isMain = false, content = "f0")
      makeFileRequest(f0, url)

      val update = VirtualFile("newName.txt", "text/txt", isMain = false, content = "new content!")
      val result = makeFileRequest(update, url + "/data.txt", PUT)

      status(result) must equalTo(OK)

      val item = testItem

      item.supportingMaterials.find(_.name == "Rubric") match {
        case Some(r) => {
          r.files.find(_.name == update.name) match {
            case Some(f) => {
              f.asInstanceOf[VirtualFile].content must equalTo(update.content)
            }
            case _ => failure("can't find updated file")
          }
        }
        case _ => throw new RuntimeException("Can't find Rubric resource")
      }
    }

    "update a file in Item.data" in {
      val url = baseItemPath(testItem.id.toString) + "/data"

      val f0 = VirtualFile("data.txt", "text/txt", isMain = false, content = "f0")
      makeFileRequest(f0, url)

      val update = VirtualFile("newName2.txt", "text/txt", isMain = false, content = "new content")
      val result = makeFileRequest(update, url + "/data.txt", PUT)

      status(result) must equalTo(OK)

      val item = testItem

      item.data.get.files.find(_.name == update.name) match {
        case Some(f) => {
          f.asInstanceOf[VirtualFile].content must equalTo(update.content)
        }
        case _ => failure("can't find updated file")
      }
    }

    "when creating a file - if its default - unsets the other items in the file list" in {
      val url = baseItemPath(testItem.id.toString) + "/materials/Rubric"

      val f0 = VirtualFile("data.file.0.default.txt", "text/txt", isMain = true, content = "f0")
      val f1 = VirtualFile("data.file.default.txt", "text/txt", isMain = true, content = "hello there")

      makeFileRequest(f0, url)
      val result = makeFileRequest(f1, url)

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
      val url = baseItemPath(testItem.id.toString) + "/data"
      val f = VirtualFile("data.file.txt", "text/txt", isMain = false, content = "hello there")

      val result = makeFileRequest(f, url)
      val json = Json.parse(contentAsString(result))
      json.as[BaseFile].name must equalTo("data.file.txt")
      status(result) must equalTo(OK)

      val secondResult = makeFileRequest(f, url)
      status(secondResult) must equalTo(NOT_ACCEPTABLE)
    }

    "create a virtual file in a supporting material resource" in {

      val url = baseItemPath(testItem.id.toString) + "/materials/Rubric"
      val f = VirtualFile("file", "text/txt", isMain = false, content = "hello there")

      val result = makeFileRequest(f, url)
      val json = Json.parse(contentAsString(result))
      json.as[BaseFile].name must equalTo("file")
      status(result) must equalTo(OK)

      val secondResult = makeFileRequest(f, url)
      status(secondResult) must equalTo(NOT_ACCEPTABLE)
    }

    "create a new supporting material resource" in {

      val url = baseItemPath(testItem.id.toString) + "/materials"
      val request = tokenRequest(POST, url, FakeHeaders(), AnyContentAsEmpty)
      routeAndCall(request) match {
        case Some(result) => {
          status(result) must equalTo(BAD_REQUEST)
        }
        case _ => failure("Request failed")
      }

      val r: Resource = Resource("newResource", Seq())

      routeAndCall(tokenRequest(POST, url, FakeHeaders(), AnyContentAsJson(Json.toJson(r)))) match {
        case Some(result) => {
          status(result) must equalTo(OK)
        }
        case _ => {
          throw new RuntimeException("Request failed")
        }
      }

      routeAndCall(tokenRequest(POST, url, FakeHeaders(), AnyContentAsJson(Json.toJson(r)))) match {
        case Some(result) => {
          contentAsString(result).contains(ApiError.ResourceNameTaken.message) must equalTo(true)
          status(result) must equalTo(NOT_ACCEPTABLE)
        }
        case _ => {
          throw new RuntimeException("Request failed")
        }
      }
      //tidy up
      routeAndCall(tokenRequest(DELETE, url + "/newResource"))
      true must equalTo(true)
    }

    "delete a new supporting material resource" in {
      val url = baseItemPath(testItem.id.toString) + "/materials"
      val r: Resource = Resource("newResource2", Seq())

      routeAndCall(tokenRequest(POST, url, FakeHeaders(), AnyContentAsJson(Json.toJson(r))))
      routeAndCall(tokenRequest(DELETE, url + "/newResource2")) match {
        case Some(result) => {
          status(result) must equalTo(OK)
        }
        case _ => throw new RuntimeException("Request Failed")
      }

    }


    "list an item's supporting materials" in {
      val url = baseItemPath(testItem.id.toString) + "/materials"
      routeAndCall(tokenRequest(GET, url)) match {
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
      val url = baseItemPath(item.id.toString) + "/materials/Rubric/" + filename + "/upload"
      val file = Play.getFile("test/files/" + filename)
      val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()

      //First upload should work
      val firstCall = call(byteArray, url, OK, filename)
      firstCall must equalTo((true, true))

      //subsequent should file beacuse the filename is taken
      val secondCall = call(byteArray, url, NOT_FOUND, ApiError.FilenameTaken.message)
      secondCall must equalTo((true, true))

      val badUrl = baseItemPath(item.id.toString) + "/materials/badResourceName/" + filename + "/upload"

      val thirdCall = call(byteArray, badUrl, NOT_FOUND, ApiError.ResourceNotFound.message)
      thirdCall must equalTo((true, true))
    }

    "do a binary post to Item.data resource" in {

      val item = testItem
      val filename = "cute-rabbit.jpg"
      val url = baseItemPath(item.id.toString) + "/" + ResourceApi.DATA_PATH + "/" + filename + "/upload"
      val file = Play.getFile("test/files/" + filename)
      val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()

      //First upload should work
      val firstCall = call(byteArray, url, OK, filename)
      firstCall must equalTo((true, true))

      //subsequent should file beacuse the filename is taken
      val secondCall = call(byteArray, url, NOT_FOUND, ApiError.FilenameTaken.message)
      secondCall must equalTo((true, true))
    }
  }
}

