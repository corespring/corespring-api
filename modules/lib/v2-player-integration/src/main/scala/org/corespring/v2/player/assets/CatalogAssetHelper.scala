package org.corespring.v2.player.assets

import org.apache.commons.httpclient.util.URIUtil
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.drafts.item.S3Paths
import org.corespring.models.appConfig.Bucket
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.player.hooks.{ CatalogAssets, PlayerAssets }
import org.corespring.v2.sessiondb.SessionServices
import play.api.libs.json.JsValue
import play.api.mvc.{ AnyContent, Request, RequestHeader, SimpleResult }

trait AssetHelper {

  def s3Service: S3Service

  def bucket: String

  protected def versionedIdFromString(itemService: ItemService, id: String): Option[VersionedId[ObjectId]] = {
    VersionedId(id).map { vid =>
      val version = vid.version.getOrElse(itemService.currentVersion(vid))
      VersionedId(vid.id, Some(version))
    }
  }

  protected def getAssetFromItemId(s3Path: String): SimpleResult = {
    val result = s3Service.download(bucket, URIUtil.decode(s3Path))
    val isOk = result.header.status / 100 == 2
    if (isOk) result else s3Service.download(bucket, s3Path)
  }
}

class CatalogAssetHelper(itemService: ItemService, val s3Service: S3Service, val bucketConfig: Bucket) extends CatalogAssets with AssetHelper {

  override def loadFile(id: String, path: String)(request: Request[AnyContent]) =
    versionedIdFromString(itemService, id).map { vid =>
      getAssetFromItemId(S3Paths.itemFile(vid, path))
    }.getOrElse(play.api.mvc.Results.BadRequest(s"Invalid versioned id: $id"))

  override def bucket: String = bucketConfig.bucket
}

class PlayerAssetHelper(
  itemService: ItemService,
  sessionServices: SessionServices,
  val s3Service: S3Service, bucketConfig: Bucket) extends PlayerAssets with AssetHelper {

  import play.api.mvc.Results.{ BadRequest, NotFound }

  override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = {
    versionedIdFromString(itemService, itemId).map { vid =>
      getAssetFromItemId(S3Paths.itemFile(vid, file))
    }.getOrElse(BadRequest(s"Invalid versioned id: $itemId"))
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]) =
    getItemIdForSessionId(id).map { vid =>
      require(vid.version.isDefined, s"The version must be defined: $vid")
      getAssetFromItemId(S3Paths.itemFile(vid, path))
    }.getOrElse(NotFound(s"Can't find an item id for session: $id"))

  private def getItemIdForSessionId(sessionId: String): Option[VersionedId[ObjectId]] = {

    def toVid(json: JsValue): Option[VersionedId[ObjectId]] = {
      val vidString = (json \ "itemId").as[String]
      val vid = VersionedId(vidString)
      require(vid.map {
        _.version.isDefined
      }.getOrElse(true), s"The version must be defined for an itemId: $vid, within a session: $sessionId")
      vid
    }

    try {
      val maybeDbo = sessionServices.main
        .load(sessionId)
        .orElse {
          sessionServices.preview.load(sessionId)
        }
      maybeDbo.map {
        toVid
      }.flatten
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        None
      }
    }
  }

  override def bucket: String = bucketConfig.bucket
}
