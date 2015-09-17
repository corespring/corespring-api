package org.corespring.platform.core.services

import com.mongodb.DBObject
import org.corespring.drafts.item.S3Paths
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.AddImageAndItem
import org.corespring.models.item.resource.{ BaseFile, StoredFile }

class ItemServiceIntegrationTest extends IntegrationSpecification {

  lazy val itemService = bootstrap.Main.itemService

  implicit val ctx = bootstrap.Main.context

  "ItemService" should {
    "saveNewUnpublishedVersion" should {

      "copy assets from the old version to the new version of an item" in
        new AddImageAndItem {
          override lazy val imagePath = "it/org/corespring/platform/core/services/ervin.png"

          val file = StoredFile("ervin.png", "image/png", false)

          val fileDbo = com.novus.salat.grater[BaseFile].asDBObject(file)
          val json =
            s"""{
               |  "$$addToSet" : {
               |    "playerDefinition.files" : ${fileDbo}
               |  }
               |}""".stripMargin

          val dbo = com.mongodb.util.JSON.parse(json).asInstanceOf[DBObject]
          itemService.saveUsingDbo(itemId, dbo)
          val path = S3Paths.itemFile(itemId, "ervin.png")
          client.getObject(bucketName, path).getKey must_== path

          itemService.saveNewUnpublishedVersion(itemId).map { newId =>
            val path = S3Paths.itemFile(newId, "ervin.png")

            logger.debug(s"check that the asset is now at the path: $path...")
            val s3Object = client.getObject(bucketName, path)
            s3Object.getKey must_== path
          }.getOrElse(failure("should have successfully saved new unpublished item"))
        }
    }

  }
}
