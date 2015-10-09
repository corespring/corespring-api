package org.corespring.v2.player.hooks

import org.apache.commons.io.IOUtils
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.container.client.hooks.{ DraftEditorHooks => ContainerDraftEditorHooks, UploadResult }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.drafts.item.{ MakeDraftId, S3Paths, ItemDrafts }
import org.corespring.drafts.item.models.{ ItemDraft, OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Validation

class DraftEditorHooks(
  transformer: ItemTransformer,
  playS3: S3Service,
  awsConfig: Bucket,
  backend: ItemDrafts,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext)
  extends ContainerDraftEditorHooks
  with LoadOrgAndOptions
  with ContainerConverters
  with MakeDraftId {

  import play.api.http.Status._

  import scalaz._

  private lazy val logger = Logger(classOf[DraftEditorHooks])

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  private def getOrgAndUser(h: RequestHeader): Validation[V2Error, OrgAndUser] = getOrgAndOptions(h).map { oo =>
    OrgAndUser(SimpleOrg.fromOrganization(oo.org), oo.user.map(SimpleUser.fromUser))
  }

  private def loadDraft(id: String)(implicit header: RequestHeader): Validation[V2Error, ItemDraft] = for {
    _ <- Success(logger.trace(s"function=loadDraft id=$id"))
    identity <- getOrgAndUser(header)
    draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
    //Note: for now we ignore conflicts
    d <- backend.loadOrCreate(identity)(draftId, ignoreConflict = true).leftMap(e => generalError(e.msg))
  } yield d

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {

    logger.trace(s"function=load id=$id")

    for {
      d <- loadDraft(id)
      item <- Success(d.change.data)
    } yield transformer.transformToV2Json(item)
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    logger.trace(s"function=loadFile id=$id path=$path")
    val result = for {
      _ <- Success(logger.trace(s"function=loadDraft id=$id"))
      identity <- getOrgAndUser(request)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
    } yield playS3.download(awsConfig.bucket, S3Paths.draftFile(draftId, path))

    result match {
      case Failure(e) => play.api.mvc.Results.Status(e.statusCode)(e.message)
      case Success(r) => r
    }
  }

  override def deleteFile(id: String, path: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.trace(s"function=deleteFile id=$id path=$path")

    val v: Validation[V2Error, DeleteResponse] = for {
      identity <- getOrgAndUser(header)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
      owns <- Success(backend.owns(identity)(draftId))
      _ <- if (owns) Success(true) else Failure(generalError(s"user: ${identity.user.map(_.userName)} from org: ${identity.org.name}, can't access $id"))
    } yield playS3.delete(awsConfig.bucket, S3Paths.draftFile(draftId, path))

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

    def loadDraftPredicate(rh: RequestHeader): Either[SimpleResult, ItemDraft] = {
      predicate(rh).fold(
        loadDraft(id)(rh).leftMap { e => Results.Status(e.statusCode)(e.message) }.toEither)(Left(_))
    }

    def addFileToData(draft: ItemDraft, key: String) = {
      val filename = grizzled.file.util.basename(key)
      val newFile = StoredFile(path, BaseFile.getContentType(filename), false, filename)
      backend.addFileToChangeSet(draft, newFile)
    }

    playS3.s3ObjectAndData[ItemDraft](awsConfig.bucket, d => S3Paths.draftFile(d.id, path))(loadDraftPredicate).map { f =>
      f.map { tuple =>
        val (s3Object, draft) = tuple
        val key = s3Object.getKey
        addFileToData(draft, key)
        IOUtils.closeQuietly(s3Object)
        UploadResult(key)
      }
    }
  }

}
