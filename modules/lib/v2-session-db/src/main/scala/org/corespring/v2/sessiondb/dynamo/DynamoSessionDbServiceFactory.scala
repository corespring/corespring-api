package org.corespring.v2.sessiondb.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import org.corespring.v2.sessiondb.SessionDbService
import org.corespring.v2.sessiondb.SessionDbServiceFactory


trait DynamoSessionDbServiceFactory extends SessionDbServiceFactory {

  val dbClient: AmazonDynamoDBClient

  lazy val dynamoDB = new DynamoDB(dbClient)

  def create(tableName: String): SessionDbService = {
    new DynamoSessionDbService(dynamoDB.getTable(tableName))
  }
}
