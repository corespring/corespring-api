package tests.api.v1

import api.ApiError
import api.v1.ResourceApi
import common.log.PackageLogging
import org.corespring.platform.core.models.item.resource.{BaseFile, VirtualFile, Resource}
import org.corespring.platform.core.models.item.service.ItemServiceImpl
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsObject
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.test.{FakeRequest, FakeHeaders}
import play.api.test.Helpers._
import scala._
import tests.BaseTest
import play.api.Play
import play.api.libs.iteratee.Iteratee
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.models.item.Item


class ResourceApiTest extends BaseTest with PackageLogging {

  sequential

  def testItemId: String = testItem.id.id.toString

  lazy val itemId : VersionedId[ObjectId]= VersionedId(ObjectId.get, None)

  def testItem: Item = {

    ItemServiceImpl.findOneById(itemId).getOrElse {

      Logger.debug("[testItem] Can't find item - create it")
      val item = Item(
        id = itemId,
        collectionId = TEST_COLLECTION_ID,
        supportingMaterials = Seq(
          Resource("Rubric", files = Seq(VirtualFile("Rubric", "text/html", true, "<html/>")))
        ),
        data = Some(Resource("data", files = Seq(VirtualFile("qti.xml", "text/xml", true, "<root/>"))))
      )
      ItemServiceImpl.save(item)
      item
    }
  }

  def rubric: Resource = {
    testItem.supportingMaterials.find(_.name == "Rubric") match {
      case Some(r) => r
      case _ => throw new RuntimeException("can't find rubric")
    }
  }

  "resource api" should {

    def makeFileRequest(file: VirtualFile, action: Action[AnyContent]): Result = {
      val request = tokenFakeRequest("blah", "blah", FakeHeaders(), AnyContentAsJson(Json.toJson(file)))
      action(request)
    }


    "delete a file from the Item.data Resource" in {
      val create = ResourceApi.createDataFile(testItemId) //.createDataFile(testItemId)
      val delete = ResourceApi.deleteDataFile(testItemId, "myfile.txt", true)
      val file = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")
      assertDelete(create, delete, testItem.data.get, file)
    }

    def assertDelete(create: Action[AnyContent], delete: Action[AnyContent], resource: => Resource, file: VirtualFile) = {
      val initialLength = resource.files.length
      Logger.debug(s"length: $initialLength")
      val result = makeFileRequest(file, create)
      Logger.debug(s"Result: ${contentAsString(result)}")
      status(result) === OK
      resource.files.length === initialLength + 1
      makeFileRequest(file, delete)
      resource.files.length must equalTo(initialLength)
    }


    "delete a file from a supportingMaterial Resource" in {
      val create = ResourceApi.createSupportingMaterialFile(testItemId, "Rubric")
      val delete = ResourceApi.deleteSupportingMaterialFile(testItemId, "Rubric", "myfile.txt")
      val file = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")
      assertDelete(create, delete, rubric, file)
    }


    "creating or updating a file to default in Item.data is ignored" in {

      val create = ResourceApi.createDataFile(testItemId)
      val f0 = VirtualFile("myfile.txt", "text/txt", isMain = true, content = "I'm never going to be main")

      makeFileRequest(f0, create)

      testItem.data.get.files.find(_.name == f0.name) match {
        case Some(f) => {
          f.isMain must equalTo(false)
        }
        case _ => failure("Can't find new file")
      }

      val update = ResourceApi.updateDataFile(testItemId, "myfile.txt", true)
      makeFileRequest(f0, update)

      testItem.data.get.files.find(_.name == f0.name) match {
        case Some(f) => {
          f.isMain must equalTo(false)
        }
        case _ => failure("Can't find new file")
      }
    }

    "update a file in supporting materials" in {
      val create = ResourceApi.createSupportingMaterialFile(testItemId, "Rubric")
      val file = VirtualFile("data.txt", "text/txt", isMain = false, content = "f0")
      val update = ResourceApi.updateSupportingMaterialFile(testItemId, "Rubric", "data.txt")
      assertUpdate(create, update, file, (_ => testItem.supportingMaterials.find(_.name == "Rubric").get))
    }

    "update a file in Item.data" in {
      val noSessionItem = "511275564924c9ca07b97043"
      val create = ResourceApi.createDataFile(noSessionItem)
      val file = VirtualFile("data.txt", "text/txt", isMain = false, content = "f0")
      val update = ResourceApi.updateDataFile(noSessionItem, "data.txt", true)
      assertUpdate(create, update, file, (_ => ItemServiceImpl.findOneById(VersionedId(new ObjectId(noSessionItem))).get.data.get))
    }

    def assertUpdate(create: Action[AnyContent], update: Action[AnyContent], file: VirtualFile, resourceFn: (Unit => Resource)) = {

      makeFileRequest(file, create)

      val updateFile = VirtualFile("newName2.txt", "text/txt", isMain = false, content = "new content")
      val result = makeFileRequest(updateFile, update)
      status(result) === OK

      resourceFn().files.find(_.name == updateFile.name) match {
        case Some(f) => {
          f.asInstanceOf[VirtualFile].content === updateFile.content
        }
        case _ => failure("can't find updated file")
      }
    }


    "when creating a file - if its default - unsets the other items in the file list" in {

      val create = ResourceApi.createSupportingMaterialFile(testItemId, "Rubric")

      val f0 = VirtualFile("data.file.0.default.txt", "text/txt", isMain = true, content = "f0")
      val f1 = VirtualFile("data.file.default.txt", "text/txt", isMain = true, content = "hello there")

      makeFileRequest(f0, create)
      val result = makeFileRequest(f1, create)

      val json = Json.parse(contentAsString(result))
      json.as[BaseFile].name === "data.file.default.txt"
      json.as[BaseFile].isMain === true

      status(result) === OK

      val item: Item = ItemServiceImpl.findOneById(testItem.id).get

      item.supportingMaterials.find(_.name == "Rubric") match {
        case Some(r) => {
          r.files.find(_.isMain == true) match {
            case Some(defaultFile) => {
              defaultFile.name === f1.name
            }
            case None => failure("couldn't find default file")
          }
        }
        case _ => failure("can't find resource")
      }
    }

    "create a virtual file in Item.data resource" in {
      val create = ResourceApi.createDataFile(testItemId)
      assertCantCreateTwoFilesWithSameName(create)
    }

    "create a virtual file in a supporting material resource" in {
      val create = ResourceApi.createSupportingMaterialFile(testItemId, "Rubric")
      assertCantCreateTwoFilesWithSameName(create)
    }

    def assertCantCreateTwoFilesWithSameName(create: Action[AnyContent]) = {
      val f = VirtualFile("data.file.txt", "text/txt", isMain = false, content = "hello there")
      val result = makeFileRequest(f, create)
      val json = Json.parse(contentAsString(result))
      json.as[BaseFile].name === "data.file.txt"
      status(result) === OK
      val secondResult = makeFileRequest(f, create)
      status(secondResult) === NOT_ACCEPTABLE
    }

    "create a new supporting material resource" in {

      val create = ResourceApi.createSupportingMaterial(testItemId)
      val request = tokenFakeRequest(FakeHeaders())
      val result = create(request)
      status(result) === BAD_REQUEST

      val r: Resource = Resource("newResource", Seq())


      val resultWithResource = create(tokenFakeRequest(FakeHeaders(), AnyContentAsJson(Json.toJson(r))))
      println(contentAsString(resultWithResource))
      println(status(resultWithResource))
      status(resultWithResource) === OK

      val secondResult = create(tokenFakeRequest(FakeHeaders(), AnyContentAsJson(Json.toJson(r))))
      contentAsString(secondResult).contains(ApiError.ResourceNameTaken.message) === true
      status(secondResult) === NOT_ACCEPTABLE

      val delete = ResourceApi.deleteSupportingMaterial(testItemId, "newResource")
      val deleteResult = delete(tokenFakeRequest())
      println(contentAsString(deleteResult))
      status(deleteResult) === OK
    }

    "delete a new supporting material resource" in {
      val resourceName = "newResource2"
      val delete = ResourceApi.deleteSupportingMaterial(testItemId, resourceName)
      val create = ResourceApi.createSupportingMaterial(testItemId)
      val r: Resource = Resource("newResource2", Seq())
      create(tokenFakeRequest(FakeHeaders(), AnyContentAsJson(Json.toJson(r))))
      val deleteResult = delete(tokenFakeRequest())
      status(deleteResult) === OK
    }


    "list an item's supporting materials" in {
      val get = ResourceApi.getSupportingMaterials(testItemId)
      val result = get(tokenFakeRequest())
      val json: JsValue = parsed[JsValue](result)
      val seq: Seq[JsObject] = json.as[Seq[JsObject]]
      seq.length === 1
      val jsItem = seq(0)
      (jsItem \ "name").asOpt[String] === Some("Rubric")
    }


    "do a binary post to supporting materials Resource" in {

      import play.api.Play.current

      val item = testItem
      val filename = "cute-rabbit.jpg"

      import VersionedIdImplicits.Binders._
      val create = api.v1.ResourceApi.uploadFile(versionedIdToString(item.id),"Rubric", filename)
      val file = Play.getFile("test/tests/files/" + filename)
      val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()


      Logger.debug(s"Test item id: ${item.id}")
      //First upload should work

      val firstCall = call(create, byteArray, OK, "cute-rabbit.jpg")
      firstCall === (true, true)

      //Second call is not acceptable - the file already exists
      val secondCall = call(create, byteArray, NOT_FOUND, ApiError.FilenameTaken.message)
      secondCall === (true, true)

      val badUpdate = api.v1.ResourceApi.uploadFile(versionedIdToString(item.id), "badResourceName", filename)
      val thirdCall = call(badUpdate, byteArray,  NOT_FOUND, ApiError.ResourceNotFound.message)
      thirdCall === (true, true)

      false === true
    }.pendingUntilFixed("Note: the assertions pass here but the assets aren't uploaded (the tests are closing the pipes)")

    def call(action : Action[String], byteArray: Array[Byte], expectedStatus: Int, expectedContains: String): (Boolean, Boolean) = {

      val result = await(action( FakeRequest(
          "",
          tokenize(""),
          FakeHeaders(Seq("Content" -> Seq("application/octet-stream"), CONTENT_LENGTH -> Seq(byteArray.length.toString))),
          byteArray
      )).run)

      Logger.debug(s"result: $result")
      Logger.debug(contentAsString(result))
      (status(result) == expectedStatus, contentAsString(result).contains(expectedContains) === true)
    }

  }
}

