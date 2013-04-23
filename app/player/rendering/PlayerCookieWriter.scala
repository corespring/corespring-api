package player.rendering

import controllers.auth.RenderOptions
import models.{UserOrg, User}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.{SimpleResult, Request}

/** Writes a player cookie to the session that grants the org Any access required */
trait PlayerCookieWriter {

  /** Write render options to the session - WIP */
 def withPlayerCookie[A,B](userId:String,providerId:String)(block:(Request[A] => SimpleResult[B]))(implicit request : Request[A]): SimpleResult[B] = {
    User.getUser(userId, providerId).map { u =>
      u.orgs match {
        case Seq(UserOrg(id,_)) => withPlayerCookie(id)(block)(request)
        case _ => block(request)
      }
    }.getOrElse(block(request))
  }

  def withPlayerCookie[A,B](orgId:ObjectId)(block:(Request[A] => SimpleResult[B]))(implicit request : Request[A]): SimpleResult[B] = {
    val newSession = request.session + ("orgId"->orgId.toString) + ("renderOptions" -> Json.toJson(RenderOptions.ANYTHING).toString)
    block(request).withSession(newSession).asInstanceOf[SimpleResult[B]]
  }
}
