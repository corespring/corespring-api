package tests.common.controllers

import org.specs2.mutable.Specification
import play.api.Logger
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.PlaySingleton
import web.controllers.ShowResource
import common.controllers.AssetResourceBase
import controllers.S3Service
import models.item.Item
import models.item.resource.BaseFile
import play.api.mvc._
import play.api.mvc.Results.Ok
import scala.Some

class AssetResourceTest extends Specification {

  PlaySingleton.start()

  val resource = new AssetResourceBase {
    def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(Action(Ok(f.toString)))
    def service: S3Service = new S3Service {
      def cloneFile(bucket: String, keyName: String, newKeyName: String) {}

      def s3upload(bucket: String, keyName: String): BodyParser[Int] = null

      def s3download(bucket: String, itemId: String, keyName: String): Result = Ok("")

      def delete(bucket: String, keyName: String): this.type#S3DeleteResponse = null

      def download(bucket: String, fullKey: String, headers: Option[Headers]): Result = Ok("fullKey: " + fullKey)

      def online: Boolean = false
    }
  }

  "show resource" should {

    "return an asset" in {
      val id = "50083ba9e4b071cb5ef79101"
      val request = FakeRequest("?","?")
      val result = resource.getResourceFile(id, "Rubric", "cute-puppy.jpg")(request)
      Logger.debug(contentAsString(result))
      status(result) === OK
      contentAsString(result) === "StoredFile(cute-puppy.jpg,image/jpg,false,test_images/cute-kittens1.jpg)"
    }

    "return an asset" in {
      val id = "50083ba9e4b071cb5ef79101"
      val request = FakeRequest("?","?")
      val result = resource.renderResource(id, "Rubric")(request)
      val expected = """VirtualFile(index.html,text/html,true,<html><head><link rel="stylesheet" href="my.css"/></head><body><div class="blue">hello world, here is a cute puppy<br/><img src="cute-puppy.jpg"/></body></html>)"""
      Logger.debug(contentAsString(result))
      status(result) === OK
      contentAsString(result) === expected
    }
  }
}
