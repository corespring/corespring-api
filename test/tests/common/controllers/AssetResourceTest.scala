package tests.common.controllers

import common.controllers.{AssetResource, AssetResourceBase}
import controllers.S3Service
import models.item.Item
import models.item.resource.BaseFile
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import tests.PlaySingleton
import models.item.service.{ItemServiceImpl, ItemService}
import org.specs2.mock.Mockito

class AssetResourceTest extends Specification with Mockito{

  PlaySingleton.start()

  val resource = new AssetResourceBase {
    def itemService : ItemService = ItemServiceImpl
    def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(Action(Ok(f.toString)))
    def s3Service: S3Service = mock[S3Service]
  }

  "show resource" should {

    val id = "50083ba9e4b071cb5ef79101"

    "return an asset" in {
      val request = FakeRequest("?","?")
      val result = resource.getResourceFile(id, "Rubric", "cute-puppy.jpg")(request)
      Logger.debug(contentAsString(result))
      status(result) === OK
      contentAsString(result) === "StoredFile(cute-puppy.jpg,image/jpg,false,test_images/cute-kittens1.jpg)"
    }

    "return an asset" in {
      val id = "50083ba9e4b071cb5ef79101"
      val request = FakeRequest("?","?")
      val result = resource.getDefaultResourceFile(id, "Rubric")(request)
      val expected = """VirtualFile(index.html,text/html,true,<html><head><link rel="stylesheet" href="my.css"/></head><body><div class="blue">hello world, here is a cute puppy<br/><img src="cute-puppy.jpg"/></body></html>)"""
      Logger.debug(contentAsString(result))
      status(result) === OK
      contentAsString(result) === expected
    }

    "give correct error message" in {

      val r = FakeRequest("?", "?")
      contentAsString(resource.getResourceFile("badId", "badResourceName", "badFileName")(r)) === AssetResource.Errors.invalidObjectId
      contentAsString(resource.getResourceFile(id.substring(0,id.length -1) + "2", "badResourceName", "badFileName")(r)) === AssetResource.Errors.cantFindItem
      contentAsString(resource.getResourceFile(id, "badResourceName", "badFileName")(r)) === AssetResource.Errors.cantFindResource
      contentAsString(resource.getResourceFile(id, "Rubric", "badFileName")(r)) === AssetResource.Errors.cantFindFileWithName + "badFileName"
    }
  }
}
