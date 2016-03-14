package org.corespring.v2.player.assets

import org.bson.types.ObjectId
import org.corespring.assets.ItemAssetKeys
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.BeforeAfter
import play.api.test.FakeRequest

import org.corespring.it.scopes.orgWithAccessTokenItemAndSession

class PlayerAssetHelperIntegrationTest extends IntegrationSpecification {

  "PlayerAssetHelper.loadItemFile" should {

    trait itemImageScope extends BeforeAfter {
      lazy val sut = main.playerAssets

      def fileName: String
      def downloadFileName = fileName

      val itemId = VersionedId(ObjectId.get, Some(0))
      val filePath = s"/test-images/$fileName"
      val s3Path = ItemAssetKeys.file(itemId, fileName)
      val imgFile = ImageUtils.resourcePathToFile(filePath)

      ImageUtils.upload(imgFile, s3Path)
      val result = sut.loadItemFile(itemId.toString(), downloadFileName)(FakeRequest())

      override def after: Any = {
        ImageUtils.delete(s3Path)
      }

      override def before: Any = {

      }
    }

    "load jpg file" in new itemImageScope {
      override def fileName = "puppy.small.jpg"
      result.header.status === 200
    }

    "load svgx file" in new itemImageScope {
      override def fileName = "circles.svgx"
      result.header.status === 200
      result.header.headers.get("Content-Type") === Some("image/svg+xml")
      result.header.headers.get("Content-Encoding") === Some("gzip")
      result.header.headers.get("Vary") === Some("Accept-Encoding")
    }

    "load file with blank in name" in new itemImageScope {
      override def fileName = "pup py.png"
      result.header.status === 200
    }

    "load file with blank in name" in new itemImageScope {
      override def fileName = "pup py.png"
      override def downloadFileName = "pup%20py.png"
      result.header.status === 200
    }

    "load file with blank in name" in new itemImageScope {
      override def fileName = "pup py.png"
      override def downloadFileName = "pup%2520py.png"
      result.header.status === 200
    }

    "load file with blank in name" in new itemImageScope {
      override def fileName = "pup%20py.png"
      result.header.status === 200
    }

    "load file with blank in name" in new itemImageScope {
      override def fileName = "pup%20py.png"
      override def downloadFileName = "pup%20py.png"
      result.header.status === 200
    }

    "load file with blank in name" in new itemImageScope {
      override def fileName = "pup%20py.png"
      override def downloadFileName = "pup%2520py.png"
      result.header.status === 200
    }


  }

  "PlayerAssetHelper.loadFile" should {

    trait sessionImageScope extends BeforeAfter with orgWithAccessTokenItemAndSession {
      lazy val sut = main.playerAssets

      def fileName: String

      val filePath = s"/test-images/$fileName"
      val s3Path = ItemAssetKeys.file(itemId, fileName)
      val imgFile = ImageUtils.resourcePathToFile(filePath)

      ImageUtils.upload(imgFile, s3Path)
      val result = sut.loadFile(sessionId.toString, fileName)(FakeRequest())

      override def after: Any = {
        ImageUtils.delete(s3Path)
      }

      override def before: Any = {

      }
    }

    "load jpg file" in new sessionImageScope {
      override def fileName = "puppy.small.jpg"
      result.header.status === 200
    }

    "load svgx file" in new sessionImageScope {
      override def fileName = "circles.svgx"
      result.header.status === 200
      result.header.headers.get("Content-Type") === Some("image/svg+xml")
      result.header.headers.get("Content-Encoding") === Some("gzip")
      result.header.headers.get("Vary") === Some("Accept-Encoding")
    }

  }


}
