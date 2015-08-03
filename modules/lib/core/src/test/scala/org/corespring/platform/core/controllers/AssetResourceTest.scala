package org.corespring.platform.core.controllers

import org.bson.types.ObjectId
import org.corespring.assets.CorespringS3Service
import org.corespring.models.item.Item
import org.corespring.models.item.resource.BaseFile
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some

class AssetResourceTest extends Specification with Mockito {

  PlaySingleton.start()

  val resource = new AssetResourceBase {
    def itemService: ItemService = ItemServiceWired
    def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(Action(Ok(f.toString)))
    def s3Service: CorespringS3Service = mock[CorespringS3Service]
  }

  "show resource" should {

    val id = "50083ba9e4b071cb5ef79101"

    "return an asset" in {
      val request = FakeRequest("?", "?")
      val result = resource.getResourceFile(id, "Rubric", "cute-puppy.jpg")(request)
      Logger.debug(contentAsString(result))
      status(result) === OK
      contentAsString(result) === "StoredFile(cute-puppy.jpg,image/jpg,false,test_images/cute-kittens1.jpg)"
    }

    "return an asset" in {
      val id = "50083ba9e4b071cb5ef79101"
      val request = FakeRequest("?", "?")
      val result = resource.getDefaultResourceFile(id, "Rubric")(request)
      val expected = """VirtualFile(index.html,text/html,true,<html><head><link rel="stylesheet" href="my.css"/></head><body><div class="blue">hello world, here is a cute puppy<br/><img src="cute-puppy.jpg"/></body></html>)"""
      Logger.debug(contentAsString(result))
      status(result) === OK
      contentAsString(result) === expected
    }

    "give correct error message" in {

      val r = FakeRequest("?", "?")
      contentAsString(resource.getResourceFile("badId", "badResourceName", "badFileName")(r)) === AssetResource.Errors.invalidObjectId
      contentAsString(resource.getResourceFile(ObjectId.get.toString, "badResourceName", "badFileName")(r)) === AssetResource.Errors.cantFindItem
      contentAsString(resource.getResourceFile(id, "badResourceName", "badFileName")(r)) === AssetResource.Errors.cantFindResource
      contentAsString(resource.getResourceFile(id, "Rubric", "badFileName")(r)) === AssetResource.Errors.cantFindFileWithName + "badFileName"
    }
  }
}
