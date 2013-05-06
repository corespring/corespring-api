package common.config

import com.typesafe.config.{Config, ConfigFactory}
import org.bson.types.ObjectId

class AppConfig(config:Config) {

  private object Keys{
    val DEMO_ORG_ID = "DEMO_ORG_ID"
    val CORESPRING_ORG_ID = "CORESPRING_ORG_ID"
  }

  lazy val demoOrgId : ObjectId = new ObjectId(config.getString(Keys.DEMO_ORG_ID))
  lazy val corespringOrgId:ObjectId = new ObjectId(config.getString(Keys.CORESPRING_ORG_ID))
}

object AppConfig extends AppConfig(ConfigFactory.load())
