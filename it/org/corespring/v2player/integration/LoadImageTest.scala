package org.corespring.v2player.integration

import com.amazonaws.auth.{ BasicAWSCredentials, AWSCredentials }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ Upload, TransferManager }
import java.io.File
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.test.helpers.models.{ V2SessionHelper, ItemHelper }
import org.specs2.mutable.BeforeAfter
import play.api.test.FakeRequest
import play.api.Logger

class LoadImageTest extends IntegrationSpecification {

  class AddImageAndItem(imagePath: String) extends BeforeAfter {

    lazy val logger = Logger("v2player.test")
    lazy val name = grizzled.file.util.basename(imagePath)
    lazy val credentials: AWSCredentials = new BasicAWSCredentials(AppConfig.amazonKey, AppConfig.amazonSecret)
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)

    lazy val itemId = ItemHelper.create(new ObjectId())
    lazy val sessionId = V2SessionHelper.create(itemId)
    lazy val bucketName = AppConfig.assetsBucket

    override def before: Any = {

      val item = ItemServiceWired.findOneById(itemId).get

      val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/data/${name}"

      val update = item.copy(
        data = Some(
          Resource(name = "data", files = Seq(
            StoredFile(name = name, contentType = "image/png", storageKey = key)))))

      ItemServiceWired.save(update)

      val file = new File(imagePath)

      require(file.exists)

      logger.debug(s"Uploading image...: ${file.getPath} -> $key")
      val upload: Upload = tm.upload(bucketName, key, file)
      upload.waitForUploadResult()
    }

    override def after: Any = {
      logger.debug(s"[after]: delete bucket: ${itemId.id}, item: $itemId, session: $sessionId")

      client.deleteObject(bucketName, s"{$itemId.id}")
      ItemHelper.delete(itemId)
      V2SessionHelper.delete(sessionId)
    }
  }

  "load image" should {

    "work" in new AddImageAndItem("it/org/corespring/v2player/integration/load-image/puppy.png") {

      import org.corespring.container.client.controllers.routes.Assets

      val call = Assets.session(sessionId.toString, "puppy.png")

      route(FakeRequest(call.method, call.url)).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))

    }
  }
}
