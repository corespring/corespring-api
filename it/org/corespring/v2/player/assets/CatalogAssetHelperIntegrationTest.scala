package org.corespring.v2.player.assets

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.assets.ItemAssetKeys
import org.corespring.drafts.item.DraftAssetKeys
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.models.item.resource.{Resource, VirtualFile}
import org.corespring.models.item.{Item, TaskInfo}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.CollectionNames
import org.specs2.mutable.BeforeAfter
import play.api.test.FakeRequest

class CatalogAssetHelperIntegrationTest extends IntegrationSpecification {

  "CatalogAssetHelper.loadFile" should {

    trait itemImageScope extends BeforeAfter {
      lazy val sut = main.catalogAssets

      def fileName: String

      val itemId = VersionedId(ObjectId.get, Some(0))
      val filePath = s"/test-images/$fileName"
      val s3Path = ItemAssetKeys.file(itemId, fileName)
      val imgFile = ImageUtils.resourcePathToFile(filePath)

      ImageUtils.upload(imgFile, s3Path)
      val result = sut.loadFile(itemId.toString(), fileName)(FakeRequest())

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

  }
}
