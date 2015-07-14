package org.corespring.v2.player

import org.apache.commons.codec.net.URLCodec
import org.corespring.it.IntegrationSpecification
import org.corespring.v2.player.scopes.{ ImageUtils, ImageUploader, orgWithAccessTokenAndItem }
import play.api.mvc.{ RawBuffer, AnyContentAsRaw }
import play.api.test.{ FakeHeaders, FakeRequest }

class UploadImageTest extends IntegrationSpecification {

  lazy val path = "it/org/corespring/v2/player/load-image/puppy.small.jpg"
  lazy val data = ImageUploader.imageData(path)

  "ItemEditor" should {

    "save uploaded asset on s3" in new orgWithAccessTokenAndItem {

      val name = "file #_<>^?.png"
      val encoded = new URLCodec().encode(name)

      val call = org.corespring.container.client.controllers.apps.routes.ItemEditor.uploadFile(itemId.toString, encoded)

      val request = FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsRaw(RawBuffer(data.length, data)))

      route(request).map { result =>
        status(result) === OK
        val list = ImageUtils.list(s"${itemId.id}")
        println(s"items: $list")
        list.length === 1
        list(0).endsWith(encoded)
      }

      override def after = {
        super.after
        println("delete assets")
        ImageUtils.delete(s"${itemId.id}")
      }
    }
  }
}
