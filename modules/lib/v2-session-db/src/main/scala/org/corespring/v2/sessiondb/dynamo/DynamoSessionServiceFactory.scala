package org.corespring.v2.sessiondb.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import org.corespring.v2.sessiondb.SessionService
import org.corespring.v2.sessiondb.SessionServiceFactory

trait DynamoSessionServiceFactory extends SessionServiceFactory {

  protected val dbClient: AmazonDynamoDBClient

  private lazy val dynamoDB = new DynamoDB(dbClient)

  def create(tableName: String): SessionService = {
    new DynamoSessionService(dynamoDB.getTable(tableName))
  }
}
