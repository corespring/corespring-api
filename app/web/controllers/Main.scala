package web.controllers

import common.controllers.session.SessionHandler
import controllers.auth.BaseApi
import play.api.mvc._
import player.accessControl.cookies.{PlayerCookieKeys, PlayerCookieWriter}
import scala.Some
import web.controllers.utils.ConfigLoader
import web.models.QtiTemplate
import models.{User, Organization}
import com.mongodb.casbah.commons.MongoDBObject
import securesocial.core.SecuredRequest


object Main extends BaseApi with PlayerCookieWriter with SessionHandler {

  def logout(s: Session) : Session = {
    s - PlayerCookieKeys.RENDER_OPTIONS
  }

  def index = SecuredAction {
    implicit request : SecuredRequest[AnyContent]=>

      val (dbServer, dbName) = getDbName(ConfigLoader.get("mongodb.default.uri"))
      val userId = request.user.id
      val user : User = User.getUser(request.user.id).getOrElse(throw new RuntimeException("Unknown user"))
      val userOrgs : Seq[Organization] = user.orgs.flatMap( o =>  Organization.findOneById(o.orgId) )
      Ok(web.views.html.index(QtiTemplate.findAll().toList, dbServer, dbName, request.user.fullName, userOrgs ))
        .withSession(
          sumSession(request.session, playerCookies(userId.id, userId.providerId) :+ activeModeCookie(): _*)
      )
  }

  private def getDbName(uri: Option[String]): (String, String) = uri match {
    case Some(url) => {
      if (!url.contains("@")) {
        val noAt = """mongodb://(.*)/(.*)""".r
        val noAt(server, name) = url
        (server, name)
      }
      else {
        val withAt = """.*@(.*)/(.*)""".r
        val withAt(server, name) = url
        (server, name)
      }
    }
    case None => ("?", "?")
  }
}

