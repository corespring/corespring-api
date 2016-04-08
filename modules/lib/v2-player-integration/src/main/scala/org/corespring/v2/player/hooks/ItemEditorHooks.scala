package org.corespring.v2.player.hooks

import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.io.IOUtils
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.container.client.hooks.{ ItemEditorHooks => ContainerItemEditorHooks, UploadResult }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.drafts.item.S3Paths
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.Validation

class ItemEditorHooks(
  transformer: ItemTransformer,
  playS3: S3Service,
  awsConfig: Bucket,
  itemAuth: ItemAuth[OrgAndOpts],
  itemService: ItemService,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext)
  extends ContainerItemEditorHooks
  with LoadOrgAndOptions {

  import play.api.http.Status._
  import scalaz._
  import scalaz.Scalaz._
  import V2ErrorToTuple._

  private lazy val logger = Logger(classOf[ItemEditorHooks])

  private val bucket = awsConfig.bucket

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  private def loadItem(id: String)(implicit header: RequestHeader) = for {
    o <- getOrgAndOptions(header)
    i <- itemAuth.loadForWrite(id)(o)
  } yield i

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.trace(s"function=load id=$id")
    for {
      item <- loadItem(id)
    } yield transformer.transformToV2Json(item)
  }

  private def getVid(id: String): Validation[V2Error, VersionedId[ObjectId]] = {

    def ensureVersionIsPresent(v: VersionedId[ObjectId]): VersionedId[ObjectId] = {
      if (v.version.isEmpty) {
        logger.warn(s"[getVid] Id is missing version: $v")
      }
      v.copy(version = v.version.orElse(Some(itemService.currentVersion(v))))
    }

    VersionedId(id)
      .map(ensureVersionIsPresent)
      .toSuccess(cantParseItemId(id))
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    logger.trace(s"function=loadFile id=$id path=$path")
    val result = for {
      _ <- Success(logger.trace(s"function=loadFile id=$id"))
      vid <- getVid(id)
    } yield playS3.download(bucket, S3Paths.itemFile(vid, path))

    result match {
      case Failure(e) => play.api.mvc.Results.Status(e.statusCode)(e.message)
      case Success(r) => r
    }
  }

  override def deleteFile(id: String, path: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.trace(s"function=deleteFile id=$id path=$path")

    def canWriteItem(id: String, identity: OrgAndOpts) = {
      itemAuth.canWrite(id)(identity) match {
        case Success(false) => Failure(generalError(s"user: ${identity.user.map(_.userName)} from org: ${identity.org.name}, can't access $id"))
        case x => x
      }
    }

    def deleteFromS3(vid: VersionedId[ObjectId], path: String) = {
      val deleteResponse = playS3.delete(bucket, S3Paths.itemFile(vid, path))
      if (deleteResponse.success) {
        Success(true)
      } else {
        Failure(generalError(deleteResponse.msg))
      }
    }

    def removeFromData(vid: VersionedId[ObjectId], key: String) = {
      val filename = grizzled.file.util.basename(key)
      val storedFile = StoredFile(path, BaseFile.getContentType(filename), false, filename)
      itemService.removeFileFromPlayerDefinition(vid, storedFile) match {
        case Success(true) => Success(true)
        case _ => Failure(generalError(s"Error removing file $path from item $vid"))
      }
    }

    val v: Validation[V2Error, Boolean] = for {
      identity <- getOrgAndOptions(header)
      canWrite <- canWriteItem(id, identity)
      vid <- getVid(id)
      deleted <- deleteFromS3(vid, path)
      removed <- removeFromData(vid, path)
    } yield removed

    v match {
      case Failure(e) => Some(e.statusCode, e.message)
      case Success(r) => None
    }
  }

  override def upload(id: String, path: String)(predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Future[UploadResult]] = {

    logger.trace(s"function=upload id=$id path=$path")

    def loadItemPredicate(rh: RequestHeader): Either[SimpleResult, Item] = {
      predicate(rh).fold(
        loadItem(id)(rh).leftMap { e => Results.Status(e.statusCode)(e.message) }.toEither)(Left(_))
    }

    def addFileToData(item: Item, key: String) = {
      val filename = grizzled.file.util.basename(key)
      val newFile = StoredFile(path, BaseFile.getContentType(filename), false, filename)
      itemService.addFileToPlayerDefinition(item, newFile)
    }

    playS3.s3ObjectAndData[Item](bucket, i => {

      if (i.id.version.isEmpty) {
        logger.warn(s"[upload] The id is missing a version: ${i.id}")
      }
      val vid: VersionedId[ObjectId] = i.id.copy(version = i.id.version.orElse(Some(itemService.currentVersion(i.id))))
      val p = S3Paths.itemFile(vid, path)
      URIUtil.encodePath(p)
    })(loadItemPredicate).map { f =>
      f.map { tuple =>
        val (s3Object, item) = tuple
        val key = s3Object.getKey
        addFileToData(item, key)
        IOUtils.closeQuietly(s3Object)
        UploadResult(URIUtil.encodePath(path))
      }
    }
  }

}
