package org.corespring.api.tracking

import akka.actor.Actor
import akka.event.Logging
import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.ApiClientService
import org.corespring.v2.auth.services.TokenService
import scalaz.Success
import org.corespring.platform.core.models.Organization

case class ApiCall(method: String, path: String, orgId: Option[String], accessToken: Option[String], clientId: Option[String]) {
  def toKeyValues = {
    s"method=$method path=$path orgId=${orgId.getOrElse("none")} accessToken=${accessToken.getOrElse("none")} clientId=${clientId.getOrElse("none")}"
  }
}

trait TrackingService {
  def log(c: => ApiCall): Unit
}

class ApiTrackingActor(trackingService: TrackingService,
  tokenService: TokenService,
  apiClientService: ApiClientService) extends Actor {

  val log = Logging(context.system, this)

  override def receive: Receive = {
    case LogRequest(rh) => {
      log.debug(s"Handle request: ${rh.path}")
      val token = rh.queryString.get("access_token").map(_.head)
      val clientId = rh.queryString.get("apiClient").map(_.head)

      val orgId: Option[ObjectId] = {
        token.map { tokenService.orgForToken(_)(rh).toOption.map(_.id) }.getOrElse {
          clientId.map { cid =>
            apiClientService.findByKey(cid).map(_.orgId)
          }.getOrElse(None)
        }
      }

      trackingService.log(ApiCall(rh.method, rh.path, orgId.map(_.toString), token, clientId))
    }
  }
}
