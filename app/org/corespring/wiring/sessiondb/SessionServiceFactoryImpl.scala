package org.corespring.wiring.sessiondb

import org.corespring.v2.sessiondb.webservice.RemoteSessionServiceFactory
import org.corespring.v2.sessiondb.{ SessionService, SessionServiceFactory }

object SessionServiceFactoryImpl extends SessionServiceFactory {

  def create(tableName: String): SessionService = RemoteSessionServiceFactory.create(tableName)

}
