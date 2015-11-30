package org.corespring.common.config

import java.net.URL

import org.bson.types.ObjectId
import play.api.Configuration
import scala.language.implicitConversions

class AppConfig(config: Configuration) {

  protected object Key extends Enumeration {
    type Key = Value
    val ALLOW_ALL_SESSIONS, AMAZON_ACCESS_KEY, AMAZON_ACCESS_SECRET, AMAZON_ASSETS_BUCKET, AMAZON_ENDPOINT, APP_VERSION_OVERRIDE, DEMO_ORG_ID, DYNAMO_DB_ACTIVATE, DYNAMO_DB_LOCAL_INIT, DYNAMO_DB_LOCAL_PORT, DYNAMO_DB_USE_LOCAL, ELASTIC_SEARCH_URL, ENV_NAME, ROOT_ORG_ID, V2_PLAYER_ORG_IDS, COMPONENT_FILTERING_ENABLED, SESSION_SERVICE, SESSION_SERVICE_URL, SESSION_SERVICE_AUTHTOKEN = Value
  }

  private implicit def keyToString(k: Key.Key): String = k.toString

  private def notFound(n: String) = throw new RuntimeException("Not found: $n")
  lazy val allowAllSessions: Boolean = config.getBoolean(Key.ALLOW_ALL_SESSIONS).getOrElse(false)
  lazy val amazonKey: String = config.getString(Key.AMAZON_ACCESS_KEY).getOrElse("?")
  lazy val amazonSecret: String = config.getString(Key.AMAZON_ACCESS_SECRET).getOrElse("?")
  lazy val amazonEndpoint: Option[String] = config.getString(Key.AMAZON_ENDPOINT)
  lazy val assetsBucket: String = config.getString(Key.AMAZON_ASSETS_BUCKET).getOrElse("?")
  lazy val appVersionOverride: String = config.getString(Key.APP_VERSION_OVERRIDE).getOrElse("")
  lazy val demoOrgId: ObjectId = config.getString(Key.DEMO_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found"))
  lazy val dynamoDbActivate: Boolean = config.getBoolean(Key.DYNAMO_DB_ACTIVATE).getOrElse(false)
  lazy val dynamoDbLocalInit: Boolean = config.getBoolean(Key.DYNAMO_DB_LOCAL_INIT).getOrElse(false)
  lazy val dynamoDbLocalPort: Int = config.getInt(Key.DYNAMO_DB_LOCAL_PORT).getOrElse(8000)
  lazy val dynamoDbUseLocal: Boolean = config.getBoolean(Key.DYNAMO_DB_USE_LOCAL).getOrElse(false)
  lazy val elasticSearchUrl: URL = new URL(config.getString(Key.ELASTIC_SEARCH_URL).getOrElse("?"))
  lazy val envName: String = config.getString(Key.ENV_NAME).getOrElse("dev")
  lazy val rootOrgId: ObjectId = config.getString(Key.ROOT_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found"))
  lazy val v2playerOrgIds: Seq[ObjectId] = config.getString(Key.V2_PLAYER_ORG_IDS).map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId])
  lazy val archiveContentCollectionId: ObjectId = new ObjectId(getString("archive.contentCollectionId"))
  lazy val archiveOrgId: ObjectId = new ObjectId(getString("archive.orgId"))
  lazy val publicSite = config.getString("publicSiteUrl").getOrElse("//www.corespring.org")
  lazy val mongoUri = getString("mongodb.default.uri")
  lazy val componentFilteringEnabled: Boolean = config.getBoolean(Key.COMPONENT_FILTERING_ENABLED).getOrElse(notFound(Key.COMPONENT_FILTERING_ENABLED))
  lazy val sessionService: String = config.getString(Key.SESSION_SERVICE).getOrElse("?")
  lazy val sessionServiceUrl: String = config.getString(Key.SESSION_SERVICE_URL).getOrElse("?")
  lazy val sessionServiceAuthToken: String = config.getString(Key.SESSION_SERVICE_AUTHTOKEN).getOrElse("?")

  private def getString(key: String): String = config.getString(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))

}

object AppConfig extends AppConfig(play.api.Play.current.configuration)
