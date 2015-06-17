package org.corespring.wiring.sessionDb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model._
import common.db.Db
import org.corespring.common.config.AppConfig
import play.api.Logger

class SessionDbTableHelper(dynamoDbClient: AmazonDynamoDBClient) {

  private val logger = Logger("org.corespring.AppWiring")

  def createTable(tableName: String, readCapacityUnits: Long = 1L, writeCapacityUnits: Long = 1L) = {

    import scala.collection.JavaConverters._

    val keySchema = Seq[KeySchemaElement](
      new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))

    val attributeDefinitions = Seq[AttributeDefinition](
      new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.S),
      new AttributeDefinition().withAttributeName("itemId").withAttributeType(ScalarAttributeType.S))

    val itemIdIndex = new GlobalSecondaryIndex()
      .withIndexName("itemId-index")
      .withKeySchema(new KeySchemaElement("itemId", KeyType.HASH))
      .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
      .withProvisionedThroughput(
        new ProvisionedThroughput()
          .withReadCapacityUnits(readCapacityUnits)
          .withWriteCapacityUnits(writeCapacityUnits))

    val request = new CreateTableRequest()
      .withTableName(tableName)
      .withKeySchema(keySchema.asJava)
      .withAttributeDefinitions(attributeDefinitions.asJavaCollection)
      .withGlobalSecondaryIndexes(itemIdIndex)
      .withProvisionedThroughput(new ProvisionedThroughput()
        .withReadCapacityUnits(readCapacityUnits)
        .withWriteCapacityUnits(writeCapacityUnits))

    dynamoDbClient.deleteTable(tableName)
    dynamoDbClient.createTable(request)
  }

  def deleteTable(tableName: String) = {
    dynamoDbClient.deleteTable(tableName)
  }

}

object SessionDbTableHelper extends SessionDbTableHelper(Db.dynamoDbClient) {

  def init(implicit app: play.api.Application) = if (AppConfig.dynamoDbUseLocal && AppConfig.dynamoDbLocalInit) {
    logger.info("Begin creating sessionDb tables")
    SessionDbTableHelper.createTable("v2.itemSessions")
    SessionDbTableHelper.createTable("v2.itemSessions_preview")
    logger.info("SessionDb tables created")
  }
}