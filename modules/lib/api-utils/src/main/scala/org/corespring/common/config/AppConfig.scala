package org.corespring.common.config

import java.net.URL

import org.bson.types.ObjectId
import play.api.Configuration
import scala.language.implicitConversions

case class AppConfig(config: Configuration) extends ConfigurationHelper {

  protected object Key extends Enumeration {
    type Key = Value
    val ALLOW_ALL_SESSIONS, AMAZON_ACCESS_KEY, AMAZON_ACCESS_SECRET, AMAZON_ASSETS_BUCKET, AMAZON_ENDPOINT, APP_VERSION_OVERRIDE, DEMO_ORG_ID, DYNAMO_DB_ACTIVATE, DYNAMO_DB_LOCAL_INIT, DYNAMO_DB_LOCAL_PORT, DYNAMO_DB_USE_LOCAL, ELASTIC_SEARCH_URL, ENV_NAME, ROOT_ORG_ID, V2_PLAYER_ORG_IDS, COMPONENT_FILTERING_ENABLED, SESSION_SERVICE, SESSION_SERVICE_URL, SESSION_SERVICE_AUTHTOKEN = Value
  }

  private implicit def keyToString(k: Key.Key): String = k.toString

  lazy val allowAllSessions: Boolean = getBoolean(Key.ALLOW_ALL_SESSIONS, Some(false))
  lazy val amazonKey: String = getString(Key.AMAZON_ACCESS_KEY, Some("?"))
  lazy val amazonSecret: String = getString(Key.AMAZON_ACCESS_SECRET, Some("?"))
  lazy val amazonEndpoint: Option[String] = getMaybeString(Key.AMAZON_ENDPOINT)
  lazy val assetsBucket: String = getString(Key.AMAZON_ASSETS_BUCKET, Some("?"))
  lazy val appVersionOverride: String = getString(Key.APP_VERSION_OVERRIDE, Some(""))
  lazy val demoOrgId: ObjectId = new ObjectId(getString(Key.DEMO_ORG_ID))
  lazy val dynamoDbActivate: Boolean = getBoolean(Key.DYNAMO_DB_ACTIVATE, Some(false))
  lazy val dynamoDbLocalInit: Boolean = getBoolean(Key.DYNAMO_DB_LOCAL_INIT, Some(false))
  lazy val dynamoDbLocalPort: Int = getInt(Key.DYNAMO_DB_LOCAL_PORT, Some(8000))
  lazy val dynamoDbUseLocal: Boolean = getBoolean(Key.DYNAMO_DB_USE_LOCAL, Some(false))
  lazy val elasticSearchUrl: URL = new URL(getString(Key.ELASTIC_SEARCH_URL, Some("?")))
  lazy val envName: String = getString(Key.ENV_NAME, Some("dev"))
  lazy val rootOrgId: ObjectId = new ObjectId(getString(Key.ROOT_ORG_ID))
  lazy val v2playerOrgIds: Seq[ObjectId] = getMaybeString(Key.V2_PLAYER_ORG_IDS).map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId])
  lazy val archiveContentCollectionId: ObjectId = new ObjectId(getString("archive.contentCollectionId"))
  lazy val archiveOrgId: ObjectId = new ObjectId(getString("archive.orgId"))
  lazy val publicSite = getString("publicSiteUrl", Some("//www.corespring.org"))
  lazy val mongoUri = getString("mongodb.default.uri")
  lazy val componentFilteringEnabled: Boolean = getBoolean(Key.COMPONENT_FILTERING_ENABLED)
  lazy val sessionService: String = getString(Key.SESSION_SERVICE, Some("?"))
  lazy val sessionServiceUrl: String = getString(Key.SESSION_SERVICE_URL, Some("?"))
  lazy val sessionServiceAuthToken: String = getString(Key.SESSION_SERVICE_AUTHTOKEN, Some("?"))


}

object AppConfig extends AppConfig(play.api.Play.current.configuration)
