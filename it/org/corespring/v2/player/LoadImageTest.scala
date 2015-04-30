package org.corespring.v2.player

import java.io.File

import com.amazonaws.auth.{ AWSCredentials, BasicAWSCredentials }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ TransferManager, Upload }
import com.mongodb.casbah.commons.MongoDBObject
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

    lazy val sessionId = V2SessionHelper.create(itemId, V2SessionHelper.v2ItemSessions)
    lazy val bucketName = AppConfig.assetsBucket

    override def before: Any = {
      import org.corespring.platform.core.models.mongoContext._

      super.before

      logger.debug(s"sessionId: $sessionId")

      val file = new File(imagePath)
      require(file.exists)

      val item = ItemServiceWired.findOneById(itemId).get
      val name = grizzled.file.util.basename(file.getCanonicalPath)
      val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/data/${name}"

      val sf = StoredFile(name = name, contentType = "image/png", storageKey = key)
      val dbo = com.novus.salat.grater[StoredFile].asDBObject(sf)

      ItemServiceWired.collection.update(
        MongoDBObject("_id._id" -> itemId.id, "_id.version" -> itemId.version.getOrElse(0)),
        MongoDBObject("$addToSet" -> MongoDBObject("data.files" -> dbo)))

      val reItem = ItemServiceWired.findOneById(itemId)
      logger.debug(s"Saved item in mongo as: ${reItem}")

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

    "return 200" in new AddImageAndItem("it/org/corespring/v2/player/load-image/puppy.png") {

      import org.corespring.container.client.controllers.apps.routes.Player

      logger.debug(s" in 'return 200' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "puppy.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse {
        failure("Can't load asset")
      }
    }

    import org.corespring.container.client.controllers.apps.routes.Player

    "return 200 when imagePath is encoded" in new AddImageAndItem("it/org/corespring/v2/player/load-image/pup%20py.png") {
      logger.debug(s" in 'return 200 when imagePath is encoded' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "pup%20py.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))
    }

    "return 200 when imagePath is not encoded" in new AddImageAndItem("it/org/corespring/v2/player/load-image/pup py.png") {
      logger.debug(s" in 'return 200 when imagePath is encoded' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "pup py.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))
    }
  }
}
