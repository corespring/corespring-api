package org.corespring.v2.player.hooks

import org.apache.commons.io.IOUtils
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.container.client.hooks.{ EditorHooks => ContainerEditorHooks, UploadResult }
import org.corespring.drafts.item.S3Paths
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

trait ItemEditorHooks
  extends ContainerEditorHooks
  with LoadOrgAndOptions {

  import play.api.http.Status._

  import scalaz._
  import scalaz.Scalaz._

  import V2ErrorToTuple._

  private lazy val logger = Logger(classOf[ItemEditorHooks])

  def transform: Item => JsValue

  def playS3: S3Service

  def bucket: String

  def itemAuth: ItemAuth[OrgAndOpts]
  def itemService: ItemService

  private def loadItem(id: String)(implicit header: RequestHeader) = for {
    o <- getOrgAndOptions(header)
    i <- itemAuth.loadForWrite(id)(o)
  } yield i

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.trace(s"function=load id=$id")
    for {
      item <- loadItem(id)
    } yield transform(item)
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    logger.trace(s"function=loadFile id=$id path=$path")
    val result = for {
      _ <- Success(logger.trace(s"function=loadFile id=$id"))
      identity <- getOrgAndOptions(request)
      _ <- Success(logger.trace(s"function=loadFile identity=$identity"))
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
    } yield playS3.download(bucket, S3Paths.itemFile(vid, path))

    result match {
      case Failure(e) => play.api.mvc.Results.Status(e.statusCode)(e.message)
      case Success(r) => r
    }
  }

  override def deleteFile(id: String, path: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.trace(s"function=deleteFile id=$id path=$path")

    val v: Validation[V2Error, DeleteResponse] = for {
      identity <- getOrgAndOptions(header)
      owns <- itemAuth.canWrite(id)(identity)
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
      _ <- if (owns) Success(true) else Failure(generalError(s"user: ${identity.user.map(_.userName)} from org: ${identity.org.name}, can't access $id"))
    } yield playS3.delete(bucket, S3Paths.itemFile(vid, path))

    v match {
      case Failure(e) => Some(e.statusCode, e.message)
      case Success(r) => {
        if (r.success) {
          None
        } else {
          Some(BAD_REQUEST, r.msg)
        }
      }
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

    playS3.s3ObjectAndData[Item](bucket, i => S3Paths.itemFile(i.id, path))(loadItemPredicate).map { f =>
      f.map { tuple =>
        val (s3Object, item) = tuple
        val key = s3Object.getKey
        addFileToData(item, key)
        IOUtils.closeQuietly(s3Object)
        UploadResult(key)
      }
    }
  }

}
