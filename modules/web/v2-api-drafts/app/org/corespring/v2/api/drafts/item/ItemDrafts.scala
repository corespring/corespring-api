package org.corespring.v2.api.drafts.item

import org.corespring.drafts.errors.{DraftError, NothingToCommit}
import org.corespring.drafts.item.models.{DraftId, OrgAndUser}
import org.corespring.drafts.item.{ItemDraftIsOutOfDate, ItemDrafts => DraftsBackend, MakeDraftId}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.drafts.item.json.{CommitJson, DraftCloneResultJson, ItemDraftJson}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.Future
import scalaz.{Failure, Success, Validation}

class ItemDrafts(
  drafts: DraftsBackend,
  identifyUser: (RequestHeader) => Option[OrgAndUser],
  itemDraftJson: ItemDraftJson) extends Controller with MakeDraftId {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scalaz.Scalaz._

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
        val seq = draftList.map(itemDraftJson.header)
        Json.toJson(seq)
      }
    }
  }

  def create(itemId: String) = Action.async { implicit request =>

    def expires: Option[DateTime] = request.body.asJson.flatMap { json =>
      (json \ "expires").asOpt[String].map { DateTime.parse }
    }

    def mkDraftName(fallback: String): String = request.body.asJson.flatMap { json =>
      (json \ "name").asOpt[String]
    }.getOrElse(fallback)

    Future {
      for {
        user <- toOrgAndUser(request)
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        draftName <- Success(mkDraftName(user.user.map(_.userName).getOrElse("unknown_user")))
        draft <- drafts.create(DraftId(vid.id, draftName, user.org.id), user, expires).leftMap { e => generalDraftApiError(e.msg) }
      } yield itemDraftJson.header(draft.toHeader)
    }
  }

  private def toApiResult(e: DraftError): DraftApiResult = e match {
    case ItemDraftIsOutOfDate(d, src) => draftIsOutOfDate(d, src.data, itemDraftJson.conflict)
    case NothingToCommit(id) => nothingToCommit(id.toString)
    case _ => generalDraftApiError(e.msg)
  }

  def commit(id: String, force: Option[Boolean] = None) = draftsAction(id, parse.empty) { (user, draftId, _) =>
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

  /**
   * Returning the item json as part of the api doesn't really make sense
   * as its only really useful in the context of the editor/dev editor
   * Check w/ ev on what to return here
   */
  def get(id: String, ignoreConflicts: Option[Boolean] = None) = draftsAction(id) { (user, draftId, rh) =>
    drafts.loadOrCreate(user)(draftId, ignoreConflicts.getOrElse(false)).bimap(
      toApiResult,
      d => itemDraftJson.withFullItem(d))
  }

  private implicit def draftErrorToDraftApiError[A](v: Validation[DraftError, A]): Validation[DraftApiError, A] = {
    v.leftMap { e => generalDraftApiError(e.msg) }
  }

  def save(draftId: String) = Action.async { implicit request =>
    Future {
      BadRequest("TODO - again does this make sense as part of the api?")
    }
  }

  def delete(id: String, all: Option[Boolean]) = draftsAction(id) { (user, draftId, _) =>

    if (all.getOrElse(false)) {
      drafts.removeByItemId(user)(draftId.itemId).bimap(
        toApiResult,
        itemId => Json.obj("itemId" -> itemId.toString,
          "id" -> draftId.toIdString))
    } else {
      drafts.remove(user)(draftId)
        .bimap(
          e => generalDraftApiError(e.msg),
          _ => Json.obj("id" -> draftId.toIdString))
    }
  }

  def getDraftsForOrg = Action.async { request =>
    Future {
      for {
        user <- toOrgAndUser(request)
      } yield {
        val orgDrafts = drafts.listForOrg(user.org.id)
        Json.toJson(orgDrafts.map(itemDraftJson.header))
      }
    }
  }

  def conflict(id: String) = draftsAction(id) { (user, draftId, _) =>
    drafts.conflict(user)(draftId)
      .bimap(
        e => generalDraftApiError(e.msg),
        c => c.map(itemDraftJson.conflict).getOrElse(Json.obj()))
  }

  private def draftsAction(id: String, parser: BodyParser[Any] = parse.anyContent)(fn: (OrgAndUser, DraftId, RequestHeader) => Validation[DraftApiResult, JsValue]) =
    Action.async(parser) { implicit request =>
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
