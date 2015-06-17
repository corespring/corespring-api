package common.db

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.mongodb.casbah.MongoDB
import org.corespring.common.config.AppConfig
import org.corespring.wiring.sessionDb.SessionDbTableHelper
import play.api._
import se.radley.plugin.salat.SalatPlugin

object Db {

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  def dynamoDbClient = {
      val client = new AmazonDynamoDBClient(new ProfileCredentialsProvider {
        def getAWSAccessKeyId: String = AppConfig.amazonKey

        def getAWSSecretKey: String = AppConfig.amazonSecret
      })
    try {
      if (AppConfig.dynamoDbUseLocal) {
        client.setEndpoint(AppConfig.dynamoDbLocalUrl)
      }
    } catch {
      case ex:Throwable => println(s"########### Error $ex")
    }
    client
  }

  implicit val current = play.api.Play.current

}
