package org.corespring.v2.sessiondb

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.mongodb.casbah.MongoDB
import org.corespring.v2.sessiondb.dynamo.DynamoSessionService
import org.corespring.v2.sessiondb.mongo.MongoSessionService

trait SessionDbModule {

  def sessionDbConfig: SessionDbConfig

  def db: MongoDB

  def awsCredentials: AWSCredentials

  private lazy val dbClient: AmazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials)

  private lazy val dynamoDB = new DynamoDB(dbClient)

  private def mkService(table: String) = {
    if (sessionDbConfig.useDynamo) {
      new DynamoSessionService(dynamoDB.getTable(table), dbClient)
    } else {
      new MongoSessionService(db(table))
    }
  }

  lazy val previewSessionService: SessionService = mkService(sessionDbConfig.previewSessionTable)
  lazy val mainSessionService: SessionService = mkService(sessionDbConfig.sessionTable)

  lazy val sessionServices: SessionServices = SessionServices(previewSessionService, mainSessionService)
}
