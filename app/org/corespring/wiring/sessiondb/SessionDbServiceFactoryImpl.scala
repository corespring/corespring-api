package org.corespring.wiring.sessiondb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.mongodb.casbah.MongoDB
import common.db.Db
import org.corespring.common.config.AppConfig
import org.corespring.v2.sessiondb.{SessionDbService, SessionDbServiceFactory}
import org.corespring.v2.sessiondb.dynamo.DynamoSessionDbServiceFactory
import org.corespring.v2.sessiondb.mongo.MongoSessionDbServiceFactory

object SessionDbServiceFactoryImpl extends SessionDbServiceFactory {

  lazy val impl = {
    if( AppConfig.dynamoDbActivate ){
      new DynamoSessionDbServiceFactory {
        val dbClient: AmazonDynamoDBClient = Db.dynamoDbClient()
      }
    } else{
      new MongoSessionDbServiceFactory {
        import play.api.Play.current

        val db:MongoDB = Db.salatDb()
      }
    }
  }

  def create(tableName: String): SessionDbService = impl.create(tableName)

}