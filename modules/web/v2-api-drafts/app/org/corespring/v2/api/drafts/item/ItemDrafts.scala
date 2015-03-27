package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors.DraftError
import org.corespring.drafts.item.models.{ ItemDraft, OrgAndUser }
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.drafts.item.json.{ CommitJson, ItemDraftJson }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, Controller, RequestHeader, SimpleResult }

import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

trait ItemDrafts extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scalaz.Scalaz._

  def drafts: DraftsBackend

  def identifyUser(rh: RequestHeader): Option[OrgAndUser]

  private def toOrgAndUser(request: RequestHeader) = identifyUser(request).toSuccess(AuthenticationFailed)

  import scala.language.implicitConversions

  implicit def validationToResult(in: Validation[DraftApiError, JsValue]): SimpleResult = {
    in match {
      case Failure(e) => Status(e.statusCode)(e.json)
      case Success(json) => Ok(json)
    }
  }

  def list(itemId: String) = Action.async { implicit request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        drafts <- Success(drafts.list(vid))
      } yield {
        val seq = drafts.map(ItemDraftJson.simple)
        Json.toJson(seq)
      }
    }
  }

  def create(itemId: String) = Action.async { implicit request =>

    def expires: Option[DateTime] = {
      request.body.asJson.flatMap { json =>
        (json \ "expires").asOpt[String].map { DateTime.parse }
      }
    }

    Future {
      for {
        user <- toOrgAndUser(request)
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        draft <- drafts.create(vid.id, user, expires).toSuccess(draftCreationFailed(itemId))
      } yield ItemDraftJson.simple(draft)
    }
  }

  def commit(draftId: ObjectId) = Action.async { implicit request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        d <- drafts.load(user)(draftId).toSuccess(UnknownDraftApiError)
        commit <- drafts.commit(user)(d).leftMap { e => UnknownDraftApiError }
      } yield {
        CommitJson(commit)
      }
    }
  }

  def get(draftId: ObjectId) = Action.async { implicit request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        d <- drafts.load(user)(draftId).toSuccess(UnknownDraftApiError)
      } yield {
        /**
         * Returning the item json as part of the api doesn't really make sense
         * as its only really useful in the context of the editor/dev editor
         * Check w/ ev on what to return here
         */
        ItemDraftJson.simple(d)
      }
    }
  }

  def save(draftId: ObjectId) = Action.async { implicit request =>
    Future {
      Ok("TODO - again does this make sense as part of the api?")
    }
  }

  def delete(draftId: ObjectId) = Action.async {
    implicit request =>
      Future {
        for {
          user <- toOrgAndUser(request)
          _ <- drafts.removeDraftByIdAndUser(draftId, user).leftMap(e => UnknownDraftApiError)
        } yield {
          Json.obj("id" -> draftId.toString)
        }
      }
  }
}