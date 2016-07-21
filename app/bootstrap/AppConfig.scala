package bootstrap

import java.net.URL

import com.amazonaws.auth.AWSCredentials
import org.bson.types.ObjectId
import play.api.Configuration

case class S3Config(credentials: AWSCredentials, bucket: String, endpoint: Option[String])

private[bootstrap] case class AppConfig(
  s3Config: S3Config,
  allowAllSessions: Boolean,
  allowExpiredTokens: Boolean,
  appVersionOverride: String,
  demoOrgId: ObjectId,
  dynamoDbActivate: Boolean,
  dynamoDbLocalInit: Boolean,
  dynamoDbLocalPort: Int,
  dynamoDbUseLocal: Boolean,
  elasticSearchUrl: URL,
  envName: String,
  rootOrgId: ObjectId,
  v2playerOrgIds: Seq[ObjectId],
  archiveContentCollectionId: ObjectId,
  archiveOrgId: ObjectId,
  publicSite: String,
  mongoUri: String,
  componentFilteringEnabled: Boolean,
  sessionService: String,
  sessionServiceUrl: String,
  sessionServiceAuthToken: String)

object AppConfig {

  private object Key extends Enumeration {
    type Key = Value
    val ALLOW_ALL_SESSIONS, ALLOW_EXPIRED_TOKENS, AMAZON_ACCESS_KEY, AMAZON_ACCESS_SECRET, AMAZON_ASSETS_BUCKET, AMAZON_ENDPOINT, APP_VERSION_OVERRIDE, DEMO_ORG_ID, DYNAMO_DB_ACTIVATE, DYNAMO_DB_LOCAL_INIT, DYNAMO_DB_LOCAL_PORT, DYNAMO_DB_USE_LOCAL, ELASTIC_SEARCH_URL, ENV_NAME, ROOT_ORG_ID, V2_PLAYER_ORG_IDS, COMPONENT_FILTERING_ENABLED, SESSION_SERVICE, SESSION_SERVICE_URL, SESSION_SERVICE_AUTHTOKEN = Value
  }

  import Key._

  import scala.language.implicitConversions

  private implicit def keyToString(k: Key.Key): String = k.toString

  def apply(config: Configuration): AppConfig = {

    def notFound(n: String) = throw new RuntimeException(s"Not found: $n")

    def getString(key: String): String = config.getString(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))

    val credentials = new AWSCredentials {
      override def getAWSAccessKeyId: String = config.getString(AMAZON_ACCESS_KEY).getOrElse("?")
      override def getAWSSecretKey: String = config.getString(AMAZON_ACCESS_SECRET).getOrElse("?")
    }

    val s3Config = S3Config(
      credentials,
      bucket = config.getString(AMAZON_ASSETS_BUCKET).getOrElse("?"),
      endpoint = config.getString(AMAZON_ENDPOINT))

    AppConfig(
      s3Config,
      allowAllSessions = config.getBoolean(ALLOW_ALL_SESSIONS).getOrElse(false),
      allowExpiredTokens = config.getBoolean(ALLOW_EXPIRED_TOKENS).getOrElse(false),
      appVersionOverride = config.getString(APP_VERSION_OVERRIDE).getOrElse(""),
      demoOrgId = config.getString(DEMO_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("demoOrgId - Not found")),
      dynamoDbActivate = config.getBoolean(DYNAMO_DB_ACTIVATE).getOrElse(false),
      dynamoDbLocalInit = config.getBoolean(DYNAMO_DB_LOCAL_INIT).getOrElse(false),
      dynamoDbLocalPort = config.getInt(DYNAMO_DB_LOCAL_PORT).getOrElse(8000),
      dynamoDbUseLocal = config.getBoolean(DYNAMO_DB_USE_LOCAL).getOrElse(false),
      elasticSearchUrl = new URL(config.getString(ELASTIC_SEARCH_URL).getOrElse("?")),
      envName = config.getString(ENV_NAME).getOrElse("dev"),
      rootOrgId = config.getString(ROOT_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("rootOrgId - Not found")),
      v2playerOrgIds = config.getString(V2_PLAYER_ORG_IDS).map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId]),
      archiveContentCollectionId = new ObjectId(getString("archive.contentCollectionId")),
      archiveOrgId = new ObjectId(getString("archive.orgId")),
      publicSite = config.getString("publicSiteUrl").getOrElse("//www.corespring.org"),
      mongoUri = getString("mongodb.default.uri"),
      componentFilteringEnabled = config.getBoolean(COMPONENT_FILTERING_ENABLED).getOrElse(notFound(COMPONENT_FILTERING_ENABLED)),
      sessionService = config.getString(SESSION_SERVICE).getOrElse("?"),
      sessionServiceUrl = config.getString(SESSION_SERVICE_URL).getOrElse("?"),
      sessionServiceAuthToken = config.getString(SESSION_SERVICE_AUTHTOKEN).getOrElse("?"))
  }
}

