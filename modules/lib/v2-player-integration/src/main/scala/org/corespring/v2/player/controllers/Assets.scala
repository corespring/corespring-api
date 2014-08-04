package org.corespring.v2.player.controllers

import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.common.config.AppConfig
import org.corespring.container.client.controllers.{ Assets => ContainerAssets }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.v2.auth.{ LoadOrgAndOptions, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.mvc.{ AnyContent, Request, SimpleResult }

trait Assets extends ContainerAssets with LoadOrgAndOptions {

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret
  private lazy val bucket = AppConfig.assetsBucket

  lazy val playS3 = new ConcreteS3Service(key, secret)

  def sessionService: MongoService

  def itemAuth: ItemAuth[OrgAndOpts]

  import scalaz.Scalaz._
  import scalaz._

  def loadAsset(itemId: String, file: String)(request: Request[AnyContent]): SimpleResult = {

    val decodedFilename = java.net.URI.create(file).getPath
    val storedFile: Validation[V2Error, StoredFile] = for {
      identity <- getOrgIdAndOptions(request)
      item <- itemAuth.loadForRead(itemId)(identity)
      data <- item.data.toSuccess(generalError(s"item doesn't contain a 'data' property': $itemId"))
      asset <- data.files.find(_.name == decodedFilename).toSuccess(generalError(s"can't find a file with name: $decodedFilename in ${data}"))
    } yield asset.asInstanceOf[StoredFile]

    storedFile match {
      case Success(sf) => {
        logger.debug(s"loadAsset: itemId: $itemId -> file: $file")
        playS3.download(bucket, sf.storageKey, Some(request.headers))
      }
      case Failure(e) => {
        logger.warn(s"can't load file: $e")
        NotFound(e.message)
      }
    }
  }

  def getItemId(sessionId: String): Option[String] = sessionService.load(sessionId).map {
    s => (s \ "itemId").as[String]
  }

}

