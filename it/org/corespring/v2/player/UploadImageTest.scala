package org.corespring.v2.player

import org.apache.commons.codec.net.URLCodec
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import play.api.mvc.{ AnyContentAsEmpty, AnyContentAsRaw, RawBuffer }
import play.api.test.{ FakeHeaders, FakeRequest }

class UploadImageTest extends IntegrationSpecification {

  lazy val path = "/test-images/puppy.small.jpg"
  lazy val data = ImageUtils.imageData(path)

  "ItemEditor" should {

    "save uploaded asset on s3" in new orgWithAccessTokenAndItem {

      val name = "file #_<>^?.png"
      val encoded = new URLCodec().encode(name)

      def uploadImage = {
        val call = org.corespring.container.client.controllers.apps.routes.ItemEditor.uploadFile(itemId.toString, encoded)
        val request = FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsRaw(RawBuffer(data.length, data)))
        route(request)
      }

      def loadImage = {
        val call = org.corespring.container.client.controllers.apps.routes.ItemEditor.getFile(itemId.toString, encoded)
        val request = FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsEmpty)
        route(request)
      }

      uploadImage.map { result =>
        status(result) === OK
        val list = ImageUtils.list(s"${itemId.id}")
        list.length === 1
        list(0).endsWith(encoded)
        loadImage.map { loadResult =>
          status(loadResult) === OK
        }
      }

      override def after = {
        super.after
        println("delete assets")
        ImageUtils.delete(s"${itemId.id}")
      }
    }
  }
}
