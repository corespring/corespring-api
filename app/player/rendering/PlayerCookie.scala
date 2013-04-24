package player.rendering

import controllers.auth.RenderOptions
import models.{UserOrg, User}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.{PlainResult, Session, SimpleResult, Request}
import models.auth.ApiClient

object PlayerCookieKeys {
  val ACTIVE_MODE = "player.active.mode"
  val RENDER_OPTIONS = "player.renderOptions"
  val ORG_ID = "player.orgId"
}

trait PlayerCookieWriter {

  def activeMode[A](mode:String)(implicit request : Request[A]) : Session = {
    request.session + (PlayerCookieKeys.ACTIVE_MODE -> mode)
  }

  def playerSession[A](userId: String, providerId: String)(implicit request: Request[A]): Session = {
    User.getUser(userId, providerId).map {
      u =>
        u.orgs match {
          case Seq(UserOrg(id, _)) => playerSession(id)
          case _ => request.session
        }
    }.getOrElse(request.session)
  }

  def playerSession[A](orgId: ObjectId)(implicit request: Request[A]): Session = playerSession(Some(orgId), Some(RenderOptions.ANYTHING))

  def playerSession[A](orgId: Option[ObjectId], options: Option[RenderOptions])(implicit request: Request[A]): Session = {
    buildSession(request.session, appendOrgId(orgId), appendOptions(options))
  }

  private def buildSession(s: Session, fns: (Session => Session)*): Session = {
    fns.foldRight(s)((fn: (Session => Session), acc: Session) => fn(acc))
  }

  private def appendOptions(options: Option[RenderOptions])(session: Session): Session = options match {
    case Some(o) => session + (PlayerCookieKeys.RENDER_OPTIONS -> Json.toJson(o).toString)
    case _ => session
  }

  private def appendOrgId(orgId: Option[ObjectId])(session: Session): Session = orgId match {
    case Some(id) => session + (PlayerCookieKeys.ORG_ID -> id.toString)
    case _ => session
  }
}

trait PlayerCookieReader {
  def activeMode[A](request:Request[A]) : Option[String] = request.session.get(PlayerCookieKeys.ACTIVE_MODE)
  def renderOptions[A](request:Request[A]) : Option[RenderOptions] =  request.session.get(PlayerCookieKeys.RENDER_OPTIONS).map{ json => Json.parse(json).as[RenderOptions] }
}
