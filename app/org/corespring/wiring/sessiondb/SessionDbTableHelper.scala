package org.corespring.wiring.sessiondb

import common.db.Db
import org.corespring.v2.sessiondb.dynamo.DynamoSessionDbTableHelper
import org.corespring.common.config.{DynamoSessionDbNames, AppConfig}
import play.api.Logger

object DynamoSessionDbInitialiser extends DynamoSessionDbTableHelper(Db.dynamoDbClient) {

  private val logger = Logger("org.corespring.AppWiring")



  def init(implicit app: play.api.Application) = {
    val tableNames = new DynamoSessionDbNames()
    if (!tableExists(tableNames.sessionTable) ||
      AppConfig.dynamoDbUseLocal && AppConfig.dynamoDbLocalInit ) {
      logger.info("Begin creating sessionDb tables")
      logger.info(s"Creating table ${tableNames.sessionTable}")
      createTable(tableNames.sessionTable)
      logger.info(s"Creating table ${tableNames.previewSessionTable}")
      createTable(tableNames.previewSessionTable)
      logger.info("SessionDb tables created")
    }
  }
}