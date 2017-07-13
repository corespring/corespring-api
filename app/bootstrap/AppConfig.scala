package bootstrap

import java.net.URL

import com.amazonaws.auth.AWSCredentials
import org.bson.types.ObjectId
import play.api.Configuration

case class S3Config(credentials: AWSCredentials, bucket: String, endpoint: Option[String])

private[bootstrap] class AppConfig(val config: Configuration) {

  object Key extends Enumeration {
    type Key = Value
    val ALLOW_ALL_SESSIONS, ALLOW_EXPIRED_TOKENS, AMAZON_ACCESS_KEY, AMAZON_ACCESS_SECRET, AMAZON_ASSETS_BUCKET, AMAZON_ENDPOINT, APP_VERSION_OVERRIDE, DEMO_ORG_ID, DYNAMO_DB_ACTIVATE, DYNAMO_DB_LOCAL_INIT, DYNAMO_DB_LOCAL_PORT, DYNAMO_DB_USE_LOCAL, ELASTIC_SEARCH_URL, ENV_NAME, ROOT_ORG_ID, V2_PLAYER_ORG_IDS, COMPONENT_FILTERING_ENABLED, SESSION_SERVICE, SESSION_SERVICE_URL, SESSION_SERVICE_AUTHTOKEN, SESSION_SERVICE_ARCHIVE_ENABLED, LOAD_EXPIRED_ITEM_DRAFTS = Value
  }

  import Key._

  import scala.language.implicitConversions

  private implicit def keyToString(k: Key.Key): String = k.toString

  private def notFound(n: String) = throw new RuntimeException(s"Not found: $n")

  private def getString(key: String): String = config.getString(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))

  lazy val credentials = new AWSCredentials {
    override def getAWSAccessKeyId: String = config.getString(AMAZON_ACCESS_KEY).getOrElse("?")

    override def getAWSSecretKey: String = config.getString(AMAZON_ACCESS_SECRET).getOrElse("?")
  }

  lazy val s3Config = S3Config(
    credentials,
    bucket = config.getString(AMAZON_ASSETS_BUCKET).getOrElse("?"),
    endpoint = config.getString(AMAZON_ENDPOINT))

  lazy val loadExpiredItemDrafts = config.getBoolean(LOAD_EXPIRED_ITEM_DRAFTS).getOrElse(false)

  lazy val allowAllSessions = config.getBoolean(ALLOW_ALL_SESSIONS).getOrElse(false)

  lazy val allowExpiredTokens = config.getBoolean(ALLOW_EXPIRED_TOKENS).getOrElse(false)

  lazy val appVersionOverride = config.getString(APP_VERSION_OVERRIDE).getOrElse("")

  lazy val demoOrgId = config.getString(DEMO_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("demoOrgId - Not found"))
  lazy val dynamoDbActivate = config.getBoolean(DYNAMO_DB_ACTIVATE).getOrElse(false)
  lazy val dynamoDbLocalInit = config.getBoolean(DYNAMO_DB_LOCAL_INIT).getOrElse(false)
  lazy val dynamoDbLocalPort = config.getInt(DYNAMO_DB_LOCAL_PORT).getOrElse(8000)
  lazy val dynamoDbUseLocal = config.getBoolean(DYNAMO_DB_USE_LOCAL).getOrElse(false)
  lazy val elasticSearchUrl = new URL(config.getString(ELASTIC_SEARCH_URL).getOrElse("?"))
  lazy val envName = config.getString(ENV_NAME).getOrElse("dev")
  lazy val rootOrgId = config.getString(ROOT_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("rootOrgId - Not found"))
  lazy val v2playerOrgIds = config.getString(V2_PLAYER_ORG_IDS).map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId])
  lazy val archiveContentCollectionId = new ObjectId(getString("archive.contentCollectionId"))
  lazy val archiveOrgId = new ObjectId(getString("archive.orgId"))
  lazy val publicSite = config.getString("publicSiteUrl").getOrElse("//www.corespring.org")
  lazy val mongoUri = getString("mongodb.default.uri")
  lazy val componentFilteringEnabled = config.getBoolean(COMPONENT_FILTERING_ENABLED).getOrElse(notFound(COMPONENT_FILTERING_ENABLED))
  lazy val sessionService = config.getString(SESSION_SERVICE).getOrElse("?")
  lazy val sessionServiceUrl = config.getString(SESSION_SERVICE_URL).getOrElse("?")
  lazy val sessionServiceAuthToken = config.getString(SESSION_SERVICE_AUTHTOKEN).getOrElse("?")
  lazy val sessionServiceArchiveEnabled = config.getBoolean(SESSION_SERVICE_ARCHIVE_ENABLED).getOrElse(false)
}

object AppConfig {
  def apply(config: Configuration): AppConfig = new AppConfig(config)
}

