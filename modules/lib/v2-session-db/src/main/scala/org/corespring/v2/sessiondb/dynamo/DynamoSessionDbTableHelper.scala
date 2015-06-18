package org.corespring.v2.sessiondb.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model._
import scala.util.control.Exception

class DynamoSessionDbTableHelper(dynamoDbClient: AmazonDynamoDBClient) {

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

    if (tableExists(tableName)) {
      deleteTable(tableName)
    }
    dynamoDbClient.createTable(request)
  }

  def deleteTable(tableName: String): Boolean =
    Exception.failAsValue[Boolean](classOf[ResourceNotFoundException])(false) {
      dynamoDbClient.deleteTable(tableName)
      true
    }

  def tableExists(tableName: String): Boolean =
    Exception.failAsValue[Boolean](classOf[ResourceNotFoundException])(false) {
      dynamoDbClient.describeTable(tableName)
      true
    }

}

