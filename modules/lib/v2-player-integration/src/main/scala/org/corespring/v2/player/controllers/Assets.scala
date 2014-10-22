package org.corespring.v2.player.controllers

import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.common.config.AppConfig
import org.corespring.container.client.controllers.{ Assets => ContainerAssets }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{Resource, StoredFile}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.mvc.{ AnyContent, Request, SimpleResult }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId

trait Assets extends ContainerAssets {

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret
  private lazy val bucket = AppConfig.assetsBucket

  lazy val playS3 = new ConcreteS3Service(key, secret)

  def sessionService: MongoService
  def previewSessionService: MongoService
  def itemService: ItemService

  import scalaz.Scalaz._
  import scalaz._

  def loadAsset(itemId: String, resourceName: String, file: String)(request: Request[AnyContent]): SimpleResult = {

    val decodedFilename = java.net.URI.create(file).getPath
    val storedFile : Validation[V2Error, StoredFile] = for {
      id <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemService.findOneById(id).toSuccess(cantFindItemWithId(id))
      dr <- getResource(item, resourceName).toSuccess(generalError("Can't find resource"))
      (isItemDataResource, resource) = dr
      file <- resource.files.find(_.name == decodedFilename).toSuccess(generalError(s"Can't find file with name $decodedFilename"))
    } yield file.asInstanceOf[StoredFile]


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

  private def getResource(item: Item, name: String): Option[(Boolean, Resource)] = if (name == Resource.DataPath) {
    item.data.map((true, _))
  } else {
    item.supportingMaterials.find(_.name == name).map((false, _))
  }

  def getItemId(sessionId: String): Option[String] = sessionService.load(sessionId).map {
    s => (s \ "itemId").as[String]
  }.orElse {
    previewSessionService.load(sessionId).map {
      s => (s \ "itemId").as[String]
    }
  }

}

