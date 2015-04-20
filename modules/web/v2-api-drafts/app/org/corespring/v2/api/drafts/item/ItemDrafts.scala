package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors.{DraftIsOutOfDate, DraftError}
import org.corespring.drafts.item.models.{ItemSrc, ItemDraft, DraftId, OrgAndUser}
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend, MakeDraftId }
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

  implicit def validationToResult(in: Validation[DraftApiError, JsValue]): SimpleResult = {
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

  def commit(id: String) = draftsAction(id) { (user, draftId) =>
    for {
      d <- drafts.load(user)(draftId).leftMap { e => generalDraftApiError(e.msg) }
      commit <- drafts.commit(user)(d).leftMap { e => generalDraftApiError(e.msg) }
    } yield CommitJson(commit)
  }

  def cloneDraft(id: String) = draftsAction(id) { (user, draftId) =>
    drafts.cloneDraft(user)(draftId).bimap(
      e => generalDraftApiError(e.msg),
      result => DraftCloneResultJson(result))
  }

  def publish(id: String) = draftsAction(id) { (user, draftId) =>
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
  def get(id: String) = draftsAction(id) { (user, draftId) =>
    drafts.loadOrCreate(user)(draftId).bimap(
      e => e match {
        case ood : DraftIsOutOfDate[ObjectId, VersionedId[ObjectId], Item] => {
            draftIsOutOfDate(ood.d.asInstanceOf[ItemDraft],ood.src.data)
        }
        case _ => generalDraftApiError(e.msg)
      },
      d => ItemDraftJson.withFullItem(d)) //.toSuccess(cantLoadDraft(draftId)).map(ItemDraftJson.simple)
  }

  private implicit def draftErrorToDraftApiError[A](v: Validation[DraftError, A]): Validation[DraftApiError, A] = {
    v.leftMap { e => generalDraftApiError(e.msg) }
  }

  def save(draftId: String) = Action.async { implicit request =>
    Future {
      BadRequest("TODO - again does this make sense as part of the api?")
    }
  }

  def delete(id: String) = draftsAction(id) { (user, draftId) =>
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

  def conflict(id: String) = draftsAction(id) { (user, draftId) =>
    drafts.conflict(user)(draftId)
      .bimap(
        e => generalDraftApiError(e.msg),
        c => c.map(ItemDraftJson.conflict).getOrElse(Json.obj()))
  }

  private def draftsAction(id: String)(fn: (OrgAndUser, DraftId) => Validation[DraftApiError, JsValue]) = Action.async { implicit request =>
    Future {
      for {
        user <- toOrgAndUser(request)
        draftId <- mkDraftId(user, id).leftMap { e => generalDraftApiError(e.msg) }
        result <- fn(user, draftId)
      } yield {
        result
      }
    }
  }

}