package org.corespring.common.config

import java.net.URL

import org.bson.types.ObjectId
import play.api.Configuration

import scala.language.implicitConversions


case class AppConfig(
                      allowAllSessions: Boolean,
                      appVersionOverride: String,
                      demoOrgId: ObjectId,
                      envName: String,
                      rootOrgId: ObjectId,
                      v2playerOrgIds: Seq[ObjectId],
                      publicSite: String,
                      componentFilteringEnabled: Boolean)

/*object AppConfig {

  private object Key extends Enumeration {
    type Key = Value
    val ALLOW_ALL_SESSIONS,
    AMAZON_ACCESS_KEY,
    AMAZON_ACCESS_SECRET,
    AMAZON_ASSETS_BUCKET,
    AMAZON_ENDPOINT,
    APP_VERSION_OVERRIDE,
    DEMO_ORG_ID,
    DYNAMO_DB_ACTIVATE,
    DYNAMO_DB_LOCAL_INIT,
    DYNAMO_DB_LOCAL_PORT,
    DYNAMO_DB_USE_LOCAL,
    ELASTIC_SEARCH_URL,
    ENV_NAME,
    ROOT_ORG_ID,
    V2_PLAYER_ORG_IDS,
    COMPONENT_FILTERING_ENABLED,
    SESSION_SERVICE,
    SESSION_SERVICE_URL,
    SESSION_SERVICE_AUTHTOKEN = Value
  }

  private implicit def keyToString(k: Key.Key): String = k.toString

  def apply(config: Configuration): AppConfig = {

    def notFound(n: String) = throw new RuntimeException("Not found: $n")

    def getString(key: String): String = config.getString(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))

    AppConfig(
      allowAllSessions = config.getBoolean(Key.ALLOW_ALL_SESSIONS).getOrElse(false),
      amazonKey = config.getString(Key.AMAZON_ACCESS_KEY).getOrElse("?"),
      amazonSecret = config.getString(Key.AMAZON_ACCESS_SECRET).getOrElse("?"),
      amazonEndpoint = config.getString(Key.AMAZON_ENDPOINT),
      assetsBucket = config.getString(Key.AMAZON_ASSETS_BUCKET).getOrElse("?"),
      appVersionOverride = config.getString(Key.APP_VERSION_OVERRIDE).getOrElse(""),
      demoOrgId = config.getString(Key.DEMO_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found")),
      dynamoDbActivate = config.getBoolean(Key.DYNAMO_DB_ACTIVATE).getOrElse(false),
      dynamoDbLocalInit = config.getBoolean(Key.DYNAMO_DB_LOCAL_INIT).getOrElse(false),
      dynamoDbLocalPort = config.getInt(Key.DYNAMO_DB_LOCAL_PORT).getOrElse(8000),
      dynamoDbUseLocal = config.getBoolean(Key.DYNAMO_DB_USE_LOCAL).getOrElse(false),
      elasticSearchUrl = new URL(config.getString(Key.ELASTIC_SEARCH_URL).getOrElse("?")),
      envName = config.getString(Key.ENV_NAME).getOrElse("dev"),
      rootOrgId = config.getString(Key.ROOT_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found")),
      v2playerOrgIds = config.getString(Key.V2_PLAYER_ORG_IDS).map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId]),
      archiveContentCollectionId = new ObjectId(getString("archive.contentCollectionId")),
      archiveOrgId = new ObjectId(getString("archive.orgId")),
      publicSite = config.getString("publicSiteUrl").getOrElse("//www.corespring.org"),
      mongoUri = getString("mongodb.default.uri"),
      componentFilteringEnabled = config.getBoolean(Key.COMPONENT_FILTERING_ENABLED).getOrElse(notFound(Key.COMPONENT_FILTERING_ENABLED)),
      sessionService = config.getString(Key.SESSION_SERVICE).getOrElse("?"),
      sessionServiceUrl = config.getString(Key.SESSION_SERVICE_URL).getOrElse("?"),
      sessionServiceAuthToken = config.getString(Key.SESSION_SERVICE_AUTHTOKEN).getOrElse("?")
    )
  }
}*/

