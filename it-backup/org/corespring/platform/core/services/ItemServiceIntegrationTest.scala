package org.corespring.platform.core.services

import org.corespring.drafts.item.S3Paths
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.v2.player.scopes.{ AddImageAndItem }

class ItemServiceIntegrationTest extends IntegrationSpecification {

  "ItemService" should {
    "saveNewUnpublishedVersion" should {

      "copy assets from the old version to the new version of an item" in
        new AddImageAndItem("it/org/corespring/platform/core/services/ervin.png") {
          val path = S3Paths.itemFile(itemId, "ervin.png")
          client.getObject(bucketName, path).getKey must_== path

          ItemServiceWired.saveNewUnpublishedVersion(itemId).map { newId =>
            val path = S3Paths.itemFile(newId, "ervin.png")
            val s3Object = client.getObject(bucketName, path)
            s3Object.getKey must_== path
          }.getOrElse(failure("should have successfully saved new unpublished item"))
        }
    }

  }
}
