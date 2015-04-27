package org.corespring.v2.player.controllers

import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.common.config.AppConfig
import org.corespring.container.client.controllers.{ Assets => ContainerAssets }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource, StoredFile }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.{ AnyContent, Request, SimpleResult }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.utils.UriEncoding

trait Assets extends ContainerAssets {

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret
  private lazy val bucket = AppConfig.assetsBucket

  lazy val logger = V2LoggerFactory.getLogger(classOf[Assets])

  lazy val playS3 = new ConcreteS3Service(key, secret)

  def sessionService: MongoService

  def previewSessionService: MongoService

  def itemService: ItemService

  import scalaz.Scalaz._
  import scalaz._

  def loadAsset(itemId: String, resourceName: String, file: String)(request: Request[AnyContent]): SimpleResult = {

    logger.debug(s"loadAsset: itemId: $itemId -> file: $file")

    def compareToFile(resource: BaseFile) = {
      //The filenames of the images uploaded in the visual editor are encoded.
      //Some elder images in the db are not encoded.
      //This comparison covers both
      resource.name == file ||
        resource.name == java.net.URI.create(file).getPath
    }
    val storedFile: Validation[V2Error, StoredFile] = for {
      id <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemService.findOneById(id).toSuccess(cantFindItemWithId(id))
      dr <- getResource(item, resourceName).toSuccess(generalError("Can't find resource"))
      (isItemDataResource, resource) = dr
      file <- resource.files.find(compareToFile).toSuccess(generalError(s"Can't find file with name $file"))
    } yield file.asInstanceOf[StoredFile]

    storedFile match {
      case Success(sf) => {
        logger.debug(s"loadAsset: itemId: $itemId, file: $file, storageKey: ${sf.storageKey}")
        playS3.download(bucket, sf.storageKey, Some(request.headers))
      }
      case Failure(e) => {
        logger.warn(s"can't load file: $file Error: $e")
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
