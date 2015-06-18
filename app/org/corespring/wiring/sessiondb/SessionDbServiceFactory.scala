package org.corespring.wiring.sessiondb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.mongodb.casbah.MongoDB
import common.db.Db
import org.corespring.v2.sessiondb.dynamo.DynamoSessionDbServiceFactory
import org.corespring.v2.sessiondb.mongo.MongoSessionDbServiceFactory

class DynamoSessionDbServiceFactoryImpl extends DynamoSessionDbServiceFactory {
  val dbClient: AmazonDynamoDBClient = Db.dynamoDbClient()
}

class MongoSessionDbServiceFactoryImpl extends MongoSessionDbServiceFactory {
  import play.api.Play.current

  val db:MongoDB = Db.salatDb()
}

object SessionDbServiceFactory extends DynamoSessionDbServiceFactoryImpl {
  //Note: If you change the db engine, you also have to change it in V2SessionHelper
}