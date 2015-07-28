package org.corespring.v2.sessiondb

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.mongodb.casbah.MongoDB
import org.corespring.common.config.AppConfig
import org.corespring.v2.sessiondb.dynamo.DynamoSessionService
import org.corespring.v2.sessiondb.mongo.MongoSessionService

case class SessionDbConfig(useDynamo: Boolean)

trait SessionDbModule {

  def sessionDBConfig: SessionDbConfig

  def db: MongoDB

  def awsCredentials: AWSCredentials

  private lazy val dbClient: AmazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials)

  private lazy val dynamoDB = new DynamoDB(dbClient)

  private def mkService(name: String) = {
    if (sessionDBConfig.useDynamo) {
      val dynamoTable = s"${AppConfig.envName}.${name}"
      new DynamoSessionService(dynamoDB.getTable(dynamoTable))
    } else {
      new MongoSessionService(db(name))
    }
  }

  lazy val previewSessionService: SessionService = mkService("v2.itemSessions_preview")
  lazy val mainSessionService: SessionService = mkService("v2.itemSessions")

  lazy val sessionServices: SessionServices = SessionServices(previewSessionService, mainSessionService)
}
