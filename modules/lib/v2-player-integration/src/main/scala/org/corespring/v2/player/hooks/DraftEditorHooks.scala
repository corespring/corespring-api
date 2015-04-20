package org.corespring.v2.player.hooks

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.container.client.hooks.{ EditorHooks => ContainerEditorHooks, UploadResult }
import org.corespring.drafts.item.{ MakeDraftId, S3Paths, ItemDrafts }
import org.corespring.drafts.item.models.{ ItemDraft, OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource, StoredFile }
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

trait DraftEditorHooks
  extends ContainerEditorHooks
  with LoadOrgAndOptions
  with DraftHelper
  with MakeDraftId {

  import play.api.http.Status._

  import scalaz.Scalaz._
  import scalaz._

  private lazy val logger = V2LoggerFactory.getLogger("DraftEditorHooks")

  def transform: Item => JsValue

  def playS3: S3Service

  def bucket: String

  def backend: ItemDrafts

  private def getOrgAndUser(h: RequestHeader): Validation[V2Error, OrgAndUser] = getOrgAndOptions(h).map { oo =>
    OrgAndUser(SimpleOrg.fromOrganization(oo.org), oo.user.map(SimpleUser.fromUser))
  }

  private def loadDraft(id: String)(implicit header: RequestHeader): Validation[V2Error, ItemDraft] = for {
    _ <- Success(logger.trace(s"function=loadDraft id=$id"))
    identity <- getOrgAndUser(header)
    draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
    //Note: for now we ignor conflicts
    d <- backend.loadOrCreate(identity)(draftId, ignoreConflict = true).leftMap { e => generalError(e.msg) }
  } yield d

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {

    logger.trace(s"function=load id=$id")

    for {
      d <- loadDraft(id)
      item <- Success(d.change.data)
    } yield transform(item)
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    logger.trace(s"function=loadFile id=$id path=$path")
    val result = for {
      _ <- Success(logger.trace(s"function=loadDraft id=$id"))
      identity <- getOrgAndUser(request)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
    } yield playS3.download(bucket, S3Paths.draftFile(draftId, path))

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
    } yield playS3.delete(bucket, S3Paths.draftFile(draftId, path))

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
      import org.corespring.platform.core.models.mongoContext.context

      draft.parent.data.data.map { d =>
        val dbo = com.novus.salat.grater[StoredFile].asDBObject(newFile)
        backend.collection.update(
          MongoDBObject("_id._id" -> draft.id),
          MongoDBObject("$addToSet" -> MongoDBObject("src.data.data.files" -> dbo)),
          false)
      }.getOrElse {

        val resource = Resource(None, "data", files = Seq(newFile))
        val resourceDbo = com.novus.salat.grater[Resource].asDBObject(resource)

        backend.collection.update(
          MongoDBObject("_id._id" -> draft.id),
          MongoDBObject("$set" -> MongoDBObject("src.data.data" -> resourceDbo)),
          false)
      }
    }

    playS3.s3ObjectAndData[ItemDraft](bucket, d => S3Paths.draftFile(d.id, path))(loadDraftPredicate).map { f =>
      f.map { tuple =>
        val (s3Object, draft) = tuple
        addFileToData(draft, s3Object.getKey)
        UploadResult(s3Object.getKey)
      }
    }
  }

}
