package web.controllers

import developer.ServiceLookup
import org.corespring.models.{User}
import play.api.mvc._
import securesocial.core.{SecuredRequest}

object Main extends Controller with securesocial.core.SecureSocial {

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"


  def index = SecuredAction {
    implicit request: SecuredRequest[AnyContent] =>

      val uri: Option[String] = play.api.Play.current.configuration.getString("mongodb.default.uri")
      val (dbServer, dbName) = getDbName(uri)
      val userId = request.user.identityId
      val user: User = ServiceLookup.userService.getUser(userId.userId, userId.providerId).getOrElse(throw new RuntimeException("Unknown user"))
      ServiceLookup.orgService.findOneById(user.org.orgId) match {
        case Some(userOrg) => Ok(web.views.html.index(dbServer, dbName, user, userOrg))
        case None => InternalServerError("could not find organization of user")
      }
  }

  private def getDbName(uri: Option[String]): (String, String) = uri match {
    case Some(url) => {
      if (!url.contains("@")) {
        val noAt = """mongodb://(.*)/(.*)""".r
        val noAt(server, name) = url
        (server, name)
      } else {
        val withAt = """.*@(.*)/(.*)""".r
        val withAt(server, name) = url
        (server, name)
      }
    }
    case None => ("?", "?")
  }
}

