package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoDB
import org.corespring.v2.sessiondb.SessionDbService
import org.corespring.v2.sessiondb.SessionDbServiceFactory

trait MongoSessionDbServiceFactory extends SessionDbServiceFactory {

  val db: MongoDB

  def create(tableName: String): SessionDbService = {
    new MongoSessionDbService(db(tableName))
  }
}