package org.corespring.wiring.sessiondb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.mongodb.casbah.MongoDB
import common.db.Db
import org.corespring.common.config.AppConfig
import org.corespring.v2.sessiondb.{ SessionService, SessionServiceFactory }
import org.corespring.v2.sessiondb.dynamo.DynamoSessionServiceFactory
import org.corespring.v2.sessiondb.mongo.MongoSessionServiceFactory

object SessionServiceFactoryImpl extends SessionServiceFactory {

  private lazy val impl = {
    if (AppConfig.dynamoDbActivate) {
      new DynamoSessionServiceFactory {
        override protected val dbClient: AmazonDynamoDBClient = Db.dynamoDbClient()
      }
    } else {
      new MongoSessionServiceFactory {
        import play.api.Play.current

        override protected val db: MongoDB = Db.salatDb()
      }
    }
  }

  def create(tableName: String): SessionService = impl.create(tableName)

}