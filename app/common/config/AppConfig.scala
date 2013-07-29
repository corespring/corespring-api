package common.config

import com.typesafe.config.{Config, ConfigFactory}
import org.bson.types.ObjectId
import scala.language.implicitConversions

class AppConfig(config:Config) {

  protected object Key extends Enumeration {
    type Key = Value
    val
    DEMO_ORG_ID,
    ROOT_ORG_ID,
    AMAZON_ACCESS_SECRET,
    AMAZON_ACCESS_KEY,
    AMAZON_ASSETS_BUCKET = Value
  }

  private implicit def keyToString(k:Key.Key) : String = k.toString

  lazy val demoOrgId : ObjectId = new ObjectId(config.getString(Key.DEMO_ORG_ID))
  lazy val rootOrgId: ObjectId = new ObjectId(config.getString(Key.ROOT_ORG_ID))
  lazy val assetsBucket : String = config.getString(Key.AMAZON_ASSETS_BUCKET)
  lazy val amazonKey : String = config.getString(Key.AMAZON_ACCESS_KEY)
  lazy val amazonSecret : String = config.getString(Key.AMAZON_ACCESS_SECRET)
}

object AppConfig extends AppConfig(ConfigFactory.load())
