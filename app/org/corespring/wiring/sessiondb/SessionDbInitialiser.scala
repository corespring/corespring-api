package org.corespring.wiring.sessiondb

import common.db.Db
import org.corespring.v2.sessiondb.dynamo.DynamoSessionDbTableHelper
import org.corespring.common.config.{ DynamoSessionDbNames, AppConfig }
import play.api.Logger

object SessionDbInitialiser {

  private val logger = Logger("org.corespring.AppWiring")

  def init(implicit app: play.api.Application) = {
    if (AppConfig.dynamoDbActivate) {
      new DynamoDbInitialiser().run()
    } else {
      initMongoDb()
    }
  }

  private def initMongoDb() = {
    //nothing to do
  }

  private class DynamoDbInitialiser {

    def run(): Unit = {
      if (!tablesExist || localDbShouldBeRecreated) {
        createTables()
      }
    }

    val helper = new DynamoSessionDbTableHelper(Db.dynamoDbClient)
    val tableNames = new DynamoSessionDbNames()

    def tablesExist = helper.tableExists(tableNames.sessionTable)
    def localDbShouldBeRecreated = AppConfig.dynamoDbUseLocal && AppConfig.dynamoDbLocalInit
    def createTables() = {
      logger.info("Begin creating dynamo sessionDb tables")
      logger.info(s"Creating table ${tableNames.sessionTable}")
      helper.createTable(tableNames.sessionTable)
      logger.info(s"Creating table ${tableNames.previewSessionTable}")
      helper.createTable(tableNames.previewSessionTable)
      logger.info("SessionDb tables created")
    }
  }

}