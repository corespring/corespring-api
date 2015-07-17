package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoDB
import org.corespring.v2.sessiondb.SessionService
import org.corespring.v2.sessiondb.SessionServiceFactory

trait MongoSessionServiceFactory extends SessionServiceFactory {

  protected val db: MongoDB

  def create(tableName: String): SessionService = {
    new MongoSessionService(db(tableName))
  }
}