package org.corespring.wiring.sessionDb

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import common.db.Db
import org.corespring.v2.auth.services.SessionDbService
import org.corespring.v2.auth.wired.{DynamoSessionDbService, MongoSessionDbService}

trait SessionDbServiceFactory {
  def create(tableName: String): SessionDbService
}

class DynamoSessionDbServiceFactory extends SessionDbServiceFactory {

  lazy val dynamoDB = new DynamoDB(Db.dynamoDbClient)

  def create(tableName: String): SessionDbService = {
    new DynamoSessionDbService(dynamoDB.getTable(tableName))
  }
}

class MongoSessionDbServiceFactory extends SessionDbServiceFactory {

  import play.api.Play.current

  def create(tableName: String): SessionDbService = {
    val db = Db.salatDb()
    new MongoSessionDbService(db(tableName))
  }
}


object SessionDbServiceFactory extends DynamoSessionDbServiceFactory {
  //Note:
  //if you change the factory to another db type,
  //you also need to change the V2SessionHelper
}