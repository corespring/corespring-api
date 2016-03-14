package org.corespring.v2.player.assets

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.drafts.item.S3Paths
import org.corespring.models.appConfig.Bucket
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.player.hooks.PlayerAssets
import org.corespring.v2.sessiondb.SessionServices
import play.api.libs.json.JsValue
import play.api.mvc.{ AnyContent, Request, RequestHeader, SimpleResult }

class PlayerAssetHelper(
  itemService: ItemService,
  sessionServices: SessionServices,
  val s3Service: S3Service,
  bucketConfig: Bucket) extends PlayerAssets with AssetHelper with ResultHeaders {

  import play.api.mvc.Results.{ BadRequest, NotFound }

  override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = {
    versionedIdFromString(itemService, itemId).map { vid =>
      getAssetFromItemId(S3Paths.itemFile(vid, file)).withContentHeaders(file)
    }.getOrElse(BadRequest(s"Invalid versioned id: $itemId"))
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]) =
    getItemIdForSessionId(id).map { vid =>
      require(vid.version.isDefined, s"The version must be defined: $vid")
      getAssetFromItemId(S3Paths.itemFile(vid, path)).withContentHeaders(path)
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
