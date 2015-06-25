package org.corespring.common.aws

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import org.corespring.common.config.AppConfig

object AwsUtil {

  def credentials() = new BasicAWSCredentials(AppConfig.amazonKey, AppConfig.amazonSecret)

  def dynamoDbClient() = {
    val client = new AmazonDynamoDBClient(credentials())
    if (AppConfig.dynamoDbUseLocal) {
      client.setEndpoint(s"localhost:${AppConfig.dynamoDbLocalPort}")
    }
    client
  }

}
