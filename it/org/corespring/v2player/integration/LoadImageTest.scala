package org.corespring.v2player.integration

import java.io.File

import com.amazonaws.auth.{ AWSCredentials, BasicAWSCredentials }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ TransferManager, Upload }
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.{ ItemHelper, V2SessionHelper }
import org.corespring.v2player.integration.scopes.{ userAndItem, SessionRequestBuilder, user }
import org.specs2.mutable.BeforeAfter
import play.api.Logger
import play.api.test.FakeRequest

class LoadImageTest extends IntegrationSpecification {

  class AddImageAndItem(imagePath: String) extends userAndItem with SessionRequestBuilder with SecureSocialHelpers {

    lazy val logger = Logger("v2player.test")
    lazy val name = grizzled.file.util.basename(imagePath)
    lazy val credentials: AWSCredentials = new BasicAWSCredentials(AppConfig.amazonKey, AppConfig.amazonSecret)
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)

    lazy val sessionId = V2SessionHelper.create(itemId)
    lazy val bucketName = AppConfig.assetsBucket

    override def before: Any = {

      super.before

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
      super.after
      logger.debug(s"[after]: delete bucket: ${itemId.id}, item: $itemId, session: $sessionId")

      client.deleteObject(bucketName, s"{$itemId.id}")
      V2SessionHelper.delete(sessionId)
    }
  }

  "load image" should {

    "work" in new AddImageAndItem("it/org/corespring/v2player/integration/load-image/puppy.png") {

      import org.corespring.container.client.controllers.routes.Assets

      val call = Assets.session(sessionId.toString, "puppy.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))

    }
  }
}
