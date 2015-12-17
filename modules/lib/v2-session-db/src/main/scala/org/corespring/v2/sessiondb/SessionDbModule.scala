package org.corespring.v2.sessiondb

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.mongodb.casbah.MongoDB
import org.corespring.sessions.SessionServiceClient
import org.corespring.v2.sessiondb.dynamo.{ DynamoSessionDbTableHelper, DynamoSessionService }
import org.corespring.v2.sessiondb.mongo.MongoSessionService
import org.corespring.v2.sessiondb.webservice.RemoteSessionService
import play.api.Logger

trait SessionDbModule {

  private lazy val logger = Logger(classOf[SessionDbModule])

  def sessionDbConfig: SessionDbConfig

  def db: MongoDB

  def awsCredentials: AWSCredentials

  def dbClient: AmazonDynamoDBClient

  private lazy val dynamoDB = new DynamoDB(dbClient)

  private def mkService(table: String) = {
    sessionDbConfig.sessionService match {
      case "remote" => {
        import scala.concurrent.ExecutionContext.Implicits.global
        val host = sessionDbConfig.sessionServiceUrl
        val authToken = sessionDbConfig.sessionServiceAuthToken
        val client = table.contains("preview") match {
          case true => new SessionServiceClient(host = host, authToken = authToken, bucket = Some("preview"))
          case _ => new SessionServiceClient(host = host, authToken = authToken, bucket = None)
        }
        new RemoteSessionService(client)
      }
      case "dynamo" => new DynamoSessionService(dynamoDB.getTable(table), dbClient)
      case _ => new MongoSessionService(db(table))
    }
  }

  lazy val previewSessionService: SessionService = mkService(sessionDbConfig.previewSessionTable)
  lazy val mainSessionService: SessionService = mkService(sessionDbConfig.sessionTable)

  lazy val sessionServices: SessionServices = SessionServices(previewSessionService, mainSessionService)

  private def initSessionDbModule() = {
    if (sessionDbConfig.useDynamo) {
      initDynamoDb()
    }
  }

  private def initDynamoDb() = {

    val helper = new DynamoSessionDbTableHelper(dbClient)
    val initLocalDynamo = sessionDbConfig.useLocalDynamo && sessionDbConfig.initLocalDynamo
    val tablesExist = helper.tableExists(sessionDbConfig.sessionTable)
    if (!tablesExist || initLocalDynamo) {
      logger.info(s"Creating table ${sessionDbConfig.sessionTable}")
      helper.createTable(sessionDbConfig.sessionTable)
      logger.info(s"Creating table ${sessionDbConfig.previewSessionTable}")
      helper.createTable(sessionDbConfig.previewSessionTable)
      logger.info("SessionDb tables created")
    }
  }

  initSessionDbModule()
}
