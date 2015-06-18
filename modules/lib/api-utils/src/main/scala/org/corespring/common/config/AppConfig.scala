package org.corespring.common.config

import java.net.URL

import org.bson.types.ObjectId
import play.api.Configuration
import scala.language.implicitConversions

class AppConfig(config: Configuration) {

  protected object Key extends Enumeration {
    type Key = Value
    val ALLOW_ALL_SESSIONS,
        AMAZON_ACCESS_KEY,
        AMAZON_ACCESS_SECRET,
        AMAZON_ASSETS_BUCKET,
        DEMO_ORG_ID,
        DYNAMO_DB_LOCAL_INIT,
        DYNAMO_DB_LOCAL_URL,
        DYNAMO_DB_USE_LOCAL,
        ELASTIC_SEARCH_URL,
        ENV_NAME,
        ROOT_ORG_ID,
        V2_PLAYER_ORG_IDS = Value
  }

  private implicit def keyToString(k: Key.Key): String = k.toString

  lazy val allowAllSessions: Boolean = config.getBoolean(Key.ALLOW_ALL_SESSIONS).getOrElse(false)
  lazy val amazonKey: String = config.getString(Key.AMAZON_ACCESS_KEY).getOrElse("?")
  lazy val amazonSecret: String = config.getString(Key.AMAZON_ACCESS_SECRET).getOrElse("?")
  lazy val assetsBucket: String = config.getString(Key.AMAZON_ASSETS_BUCKET).getOrElse("?")
  lazy val demoOrgId: ObjectId = config.getString(Key.DEMO_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found"))
  lazy val dynamoDbLocalInit:Boolean = config.getBoolean(Key.DYNAMO_DB_LOCAL_INIT).getOrElse(false)
  lazy val dynamoDbLocalUrl:String = config.getString(Key.DYNAMO_DB_LOCAL_URL).getOrElse("?")
  lazy val dynamoDbUseLocal:Boolean = config.getBoolean(Key.DYNAMO_DB_USE_LOCAL).getOrElse(false)
  lazy val elasticSearchUrl: URL = new URL(config.getString(Key.ELASTIC_SEARCH_URL).getOrElse("?"))
  lazy val envName:String = config.getString(Key.ENV_NAME).getOrElse("dev")
  lazy val rootOrgId: ObjectId = config.getString(Key.ROOT_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found"))
  lazy val v2playerOrgIds: Seq[ObjectId] = config.getString(Key.V2_PLAYER_ORG_IDS).map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId])

}

object AppConfig extends AppConfig(play.api.Play.current.configuration)
