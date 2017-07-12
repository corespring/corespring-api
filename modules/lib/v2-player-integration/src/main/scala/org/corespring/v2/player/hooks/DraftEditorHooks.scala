package org.corespring.v2.player.hooks

import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.io.IOUtils
import org.corespring.amazon.s3.S3Service
import org.corespring.common.url.EncodingHelper
import org.corespring.container.client.hooks.{ UploadResult, DraftEditorHooks => ContainerDraftEditorHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{ DraftAssetKeys, ItemDrafts, MakeDraftId, S3Paths }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.{ DisplayConfigJson, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.assets.S3PathResolver
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Validation

class DraftEditorHooks(
  transformer: ItemTransformer,
  playS3: S3Service,
  s3PathResolver: S3PathResolver,
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

  private lazy val logger = Logger(this.getClass)

  private lazy val encodingHelper = new EncodingHelper()

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

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    logger.trace(s"function=load id=$id")
    for {
      identity <- getOrgAndOptions(header)
      d <- loadDraft(id)
      identity <- getOrgAndOptsFn(header)
      item <- Success(d.change.data)
    } yield (transformer.transformToV2Json(item), DisplayConfigJson(identity))
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    logger.trace(s"function=loadFile id=$id path=$path, bucket=${awsConfig.bucket}")

    def onIdentity(identity: OrgAndUser): Validation[V2Error, SimpleResult] = {
      logger.trace(s"function=loadFile, id=$id, identity=$identity")
      for {
        draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
      } yield playS3.download(awsConfig.bucket, S3Paths.draftFile(draftId, path))
    }

    /**
     * If user isn't authenticated get the name from the id string and resolve the path using the [[S3PathResolver]].
     * see: https://thesib.atlassian.net/browse/AC-324
     * The only time this should happen is when the draft editor is launched via the js api.
     * We may want to review how we handle assets for drafts, but for now this is the path of least resistance.
     * @param e
     * @return
     */
    def onNoIdentity(e: V2Error): Validation[V2Error, SimpleResult] = {
      logger.info(s"function=loadFile, id=$id - no identity found - resolve path using wildcard")

      DraftId.idStringToObjectIdAndUserName(id) match {
        case Some((itemId, name)) => {
          val paths = s3PathResolver.resolve(DraftAssetKeys.draftItemIdFolder(itemId), s".*/$name/.*?/$path")
          paths match {
            case Seq(p) => Success(playS3.download(awsConfig.bucket, p))
            case _ => Failure(generalError("Can't find asset", NOT_FOUND))
          }
        }
        case None => Failure(generalError(s"Can't read the id as a draftId: $id"))
      }
    }

    val result: Validation[V2Error, SimpleResult] = getOrgAndUser(request).fold(onNoIdentity, onIdentity)

    result match {
      case Failure(e) => play.api.mvc.Results.Status(e.statusCode)(e.message)
      case Success(r) => r
    }
  }

  override def deleteFile(id: String, path: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.trace(s"function=deleteFile id=$id path=$path")

    def deleteFromS3(draftId: DraftId, path: String) = {
      val deleteResponse = playS3.delete(awsConfig.bucket, S3Paths.draftFile(draftId, path))
      if (deleteResponse.success) {
        Success(true)
      } else {
        Failure(generalError(deleteResponse.msg))
      }
    }

    def removeFromData(draftId: DraftId, path: String) = {
      val filename = grizzled.file.util.basename(path)
      val file = StoredFile(path, BaseFile.getContentType(filename), false, filename)
      if (backend.removeFileFromChangeSet(draftId, file)) {
        Success(true)
      } else {
        Failure(generalError(s"Error removing file $path from draft $draftId"))
      }
    }

    val v: Validation[V2Error, Boolean] = for {
      identity <- getOrgAndUser(header)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
      owns <- if (backend.owns(identity)(draftId)) Success(true) else Failure(generalError(s"user: ${identity.user.map(_.userName)} from org: ${identity.org.name}, can't access $id"))
      deletedFromS3 <- deleteFromS3(draftId, path)
      removedFromData <- removeFromData(draftId, path)
    } yield removedFromData

    v match {
      case Failure(e) => Some(e.statusCode, e.message)
      case _ => None
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

    def urlEncode(s: String): String = {
      encodingHelper.encodedOnce(s)
    }

    playS3.uploadWithData[ItemDraft](awsConfig.bucket, d => {
      val p = S3Paths.draftFile(d.id, path)
      urlEncode(p)
    })(loadDraftPredicate).map { f =>
      f.map { tuple =>
        val (upload, draft) = tuple
        addFileToData(draft, upload.key)
        UploadResult(urlEncode(path))
      }
    }
  }

}
