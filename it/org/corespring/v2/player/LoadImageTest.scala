package org.corespring.v2.player

import java.io.File

import com.amazonaws.auth.{ AWSCredentials, BasicAWSCredentials }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ TransferManager, Upload }
import org.corespring.common.config.AppConfig
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.player.scopes.{ SessionRequestBuilder, userAndItem }
import play.api.Logger

class LoadImageTest extends IntegrationSpecification {

  class AddImageAndItem(imagePath: String) extends userAndItem with SessionRequestBuilder with SecureSocialHelpers {

    lazy val logger = Logger("v2player.test")
    lazy val credentials: AWSCredentials = new BasicAWSCredentials(AppConfig.amazonKey, AppConfig.amazonSecret)
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)

    lazy val sessionId = V2SessionHelper.create(itemId)
    lazy val bucketName = AppConfig.assetsBucket

    override def before: Any = {

      super.before

      val file = new File(imagePath)
      require(file.exists)

      val item = ItemServiceWired.findOneById(itemId).get
      val name = grizzled.file.util.basename(file.getCanonicalPath)
      val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/data/${name}"

      val update = item.copy(
        data = Some(
          Resource(name = "data", files = Seq(
            StoredFile(name = name, contentType = "image/png", storageKey = key)))))

      ItemServiceWired.save(update)

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

    "work" in new AddImageAndItem("it/org/corespring/v2/player/load-image/puppy.png") {

      import org.corespring.container.client.controllers.routes.Assets

      val call = Assets.session(sessionId.toString, "puppy.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))

    }
  }
}
