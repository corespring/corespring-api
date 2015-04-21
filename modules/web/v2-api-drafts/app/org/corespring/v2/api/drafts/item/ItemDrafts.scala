package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors.{ NothingToCommit, DraftIsOutOfDate, DraftError }
import org.corespring.drafts.item.models.{ ItemSrc, ItemDraft, DraftId, OrgAndUser }
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend, ItemDraftIsOutOfDate, MakeDraftId }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.drafts.item.json.{ DraftCloneResultJson, CommitJson, ItemDraftJson }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, Controller, RequestHeader, SimpleResult }

import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

trait ItemDrafts extends Controller with MakeDraftId {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scalaz.Scalaz._

  def drafts: DraftsBackend

  def identifyUser(rh: RequestHeader): Option[OrgAndUser]

  private def toOrgAndUser(request: RequestHeader) = identifyUser(request).toSuccess(AuthenticationFailed)

  import scala.language.implicitConversions

  implicit def validationToResult(in: Validation[DraftApiResult, JsValue]): SimpleResult = {
    in match {
      case Failure(e) => Status(e.statusCode)(e.json)
      case Success(json) => Ok(json)
    }
  }

  def listByItem(itemId: String) = Action.async { implicit request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        draftList <- Success(drafts.listByItemAndOrgId(vid, user.org.id))
      } yield {
        val seq = draftList.map(ItemDraftJson.simple)
        Json.toJson(seq)
      }
    }
  }

  def create(itemId: String) = Action.async { implicit request =>

    def expires: Option[DateTime] = request.body.asJson.flatMap { json =>
      (json \ "expires").asOpt[String].map { DateTime.parse }
    }

    Future {
      for {
        user <- toOrgAndUser(request)
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        draft <- drafts.create(vid, user, expires).leftMap { e => generalDraftApiError(e.msg) }
      } yield ItemDraftJson.simple(draft)
    }
  }

  private def toApiResult(e: DraftError): DraftApiResult = e match {
    case ItemDraftIsOutOfDate(d, src) => draftIsOutOfDate(d, src.data)
    case NothingToCommit(id) => nothingToCommit(id.toString)
    case _ => generalDraftApiError(e.msg)
  }

  def commit(id: String, force: Option[Boolean] = None) = draftsAction(id) { (user, draftId, _) =>
    for {
      d <- drafts.load(user)(draftId).leftMap(toApiResult)
      commit <- drafts.commit(user)(d, force.getOrElse(false)).leftMap(toApiResult)
    } yield CommitJson(commit)
  }

  def cloneDraft(id: String) = draftsAction(id) { (user, draftId, _) =>
    drafts.cloneDraft(user)(draftId).bimap(
      e => generalDraftApiError(e.msg),
      result => DraftCloneResultJson(result))
  }

  def publish(id: String) = draftsAction(id) { (user, draftId, _) =>
    for {
      vid <- drafts.publish(user)(draftId).leftMap(e => generalDraftApiError(e.msg))
    } yield {
      Json.obj("itemId" -> vid.toString)
    }
  }

  /**
   * Returning the item json as part of the api doesn't really make sense
   * as its only really useful in the context of the editor/dev editor
   * Check w/ ev on what to return here
   */
  def get(id: String, ignoreConflicts: Option[Boolean] = None) = draftsAction(id) { (user, draftId, rh) =>
    drafts.loadOrCreate(user)(draftId, ignoreConflicts.getOrElse(false)).bimap(
      toApiResult,
      d => ItemDraftJson.withFullItem(d))
  }

  private implicit def draftErrorToDraftApiError[A](v: Validation[DraftError, A]): Validation[DraftApiError, A] = {
    v.leftMap { e => generalDraftApiError(e.msg) }
  }

  def save(draftId: String) = Action.async { implicit request =>
    Future {
      BadRequest("TODO - again does this make sense as part of the api?")
    }
  }

  def delete(id: String) = draftsAction(id) { (user, draftId, _) =>
    drafts.remove(user)(draftId)
      .bimap(
        e => generalDraftApiError(e.msg),
        _ => Json.obj("id" -> draftId.toString))
  }

  def getDraftsForOrg = Action.async { request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        orgDrafts <- Success(drafts.listForOrg(user.org.id))
      } yield Json.toJson(orgDrafts.map { ItemDraftJson.simple })
    }
  }

  def conflict(id: String) = draftsAction(id) { (user, draftId, _) =>
    drafts.conflict(user)(draftId)
      .bimap(
        e => generalDraftApiError(e.msg),
        c => c.map(ItemDraftJson.conflict).getOrElse(Json.obj()))
  }

  private def draftsAction(id: String)(fn: (OrgAndUser, DraftId, RequestHeader) => Validation[DraftApiResult, JsValue]) = Action.async { implicit request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        draftId <- mkDraftId(user, id).leftMap { e => generalDraftApiError(e.msg) }
        result <- fn(user, draftId, request)
      } yield {
        result
      }
    }
  }

}
