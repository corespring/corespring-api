package org.corespring.v2.sessiondb.webservice

import org.corespring.common.config.AppConfig
import org.corespring.v2.sessiondb.{ SessionService, SessionServiceFactory }

object RemoteSessionServiceFactory extends SessionServiceFactory {

  val host = AppConfig.sessionServiceUrl
  val authToken = AppConfig.sessionServiceAuthToken

  override def create(tableName: String): SessionService = {
    tableName.contains("preview") match {
      case true => new RemoteSessionService(host = host, authToken = authToken, bucket = Some("preview"))
      case _ => new RemoteSessionService(host = host, authToken = authToken, bucket = None)
    }
  }

}
