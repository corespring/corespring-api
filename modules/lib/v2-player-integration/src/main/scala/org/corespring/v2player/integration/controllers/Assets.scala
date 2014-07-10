package org.corespring.v2player.integration.controllers

import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.common.config.AppConfig
import org.corespring.container.client.controllers.{ Assets => ContainerAssets }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.ItemAuth
import play.api.mvc.{ AnyContent, Request, SimpleResult }

trait Assets extends ContainerAssets {

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret
  private lazy val bucket = AppConfig.assetsBucket

  lazy val playS3 = new ConcreteS3Service(key, secret)

  def sessionService: MongoService

  def itemAuth: ItemAuth
  import scalaz.Scalaz._
  import scalaz._

  def loadAsset(itemId: String, file: String)(request: Request[AnyContent]): SimpleResult = {

    val decodedFilename = java.net.URI.create(file).getPath
    val storedFile: Validation[String, StoredFile] = for {
      item <- itemAuth.loadForRead(itemId)(request)
      data <- item.data.toSuccess(s"item doesn't contain a 'data' property': $itemId")
      asset <- data.files.find(_.name == decodedFilename).toSuccess(s"can't find a file with name: $decodedFilename in ${data}")
    } yield asset.asInstanceOf[StoredFile]

    storedFile match {
      case Success(sf) => {
        logger.debug(s"loadAsset: itemId: $itemId -> file: $file")
        playS3.download(bucket, sf.storageKey, Some(request.headers))
      }
      case Failure(msg) => {
        logger.warn(s"can't load file: $msg")
        NotFound(msg)
      }
    }
  }

  def getItemId(sessionId: String): Option[String] = sessionService.load(sessionId).map {
    s => (s \ "itemId").as[String]
  }

}

