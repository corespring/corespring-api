package org.corespring.wiring.sessiondb

import common.db.Db
import org.corespring.v2.sessiondb.dynamo.DynamoSessionDbTableHelper
import org.corespring.common.config.AppConfig
import play.api.Logger


object DynamoSessionDbInitialiser extends DynamoSessionDbTableHelper(Db.dynamoDbClient) {

  private val logger = Logger("org.corespring.AppWiring")

  def init(implicit app: play.api.Application) = if (AppConfig.dynamoDbUseLocal && AppConfig.dynamoDbLocalInit) {
    logger.info("Begin creating sessionDb tables")
    createTable("v2.itemSessions")
    createTable("v2.itemSessions_preview")
    logger.info("SessionDb tables created")
  }
}