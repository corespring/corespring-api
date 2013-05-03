package common.config

import com.typesafe.config.{Config, ConfigFactory}

class AppConfig(config:Config) {

  private object Keys{
    val DEMO_ORG_ID = "DEMO_ORG_ID"
  }

  lazy val demoOrgId : String = config.getString(Keys.DEMO_ORG_ID)
}

object AppConfig extends AppConfig(ConfigFactory.load())
