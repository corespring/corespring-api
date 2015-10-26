package common.db

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.mongodb.casbah.MongoDB
import org.corespring.common.aws.AwsUtil
import play.api._
import se.radley.plugin.salat.SalatPlugin

object Db {

  implicit val current = play.api.Play.current

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(
      throw new PlayException(
        "SalatPlugin is not registered.",
        "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  def dynamoDbClient(): AmazonDynamoDBClient = AwsUtil.dynamoDbClient()

}

