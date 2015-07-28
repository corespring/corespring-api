package bootstrap

import bootstrap.Actors.UpdateItem
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client }
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import common.db.Db
import developer.ServiceLookup
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.drafts.item.models.OrgAndUser
import org.corespring.encryption.EncryptionModule
import org.corespring.itemSearch.{ ElasticSearchUrl, ElasticSearchExecutionContext, ItemSearchModule }
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ ComponentType, FieldValue }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap._
import org.corespring.v2.api.{ ItemSessionApiExecutionContext, ItemApiExecutionContext, V2ApiModule }
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.{ AccessSettingsWildcardCheck, V2AuthModule }
import org.corespring.v2.auth.models.{ Mode, PlayerAccessSettings, OrgAndOpts }
import org.corespring.v2.auth.wired.{ HasPermissions, SessionServices }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.{ V2PlayerModule, AllItemVersionTransformer }
import play.api.Play
import play.api.mvc.{ RequestHeader, Controller }

import scala.concurrent.ExecutionContext
import scalaz.Validation

object Main
  extends SalatServices
  with EncryptionModule
  with ItemSearchModule
  with V2AuthModule
  with V2ApiModule
  with V2PlayerModule {

  import play.api.Play.current

  lazy val configuration = current.configuration

  override lazy val elasticSearchUrl: ElasticSearchUrl = ElasticSearchUrl(AppConfig.elasticSearchUrl)

  override lazy val elasticSearchExecutionContext: ElasticSearchExecutionContext = ElasticSearchExecutionContext(ExecutionContext.Implicits.global)

  //session auth
  override lazy val perms: HasPermissions = new HasPermissions {
    override def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      AccessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)
    }
  }

  override lazy val itemTransformer: ItemTransformer = new AllItemVersionTransformer(
    this,
    db("content"),
    db("versioned_content"),
    AppConfig.rootOrgId,
    context)

  override lazy val sessionServices: SessionServices = ???

  override lazy val sessionCreatedCallback: VersionedId[ObjectId] => Unit = {
    (itemId) =>
      Actors.itemTransformerActor ! UpdateItem(itemId)
  }

  override lazy val componentTypes: Seq[ComponentType] = ???

  override lazy val itemSessionApiExecutionContext: ItemSessionApiExecutionContext = ItemSessionApiExecutionContext(ExecutionContext.Implicits.global)

  override lazy val getOrgAndOptsFn: (RequestHeader) => Validation[V2Error, OrgAndOpts] = ???

  override lazy val itemApiExecutionContext: ItemApiExecutionContext = ItemApiExecutionContext(ExecutionContext.global)

  override lazy val score: ScoreService = ???

  lazy val aws = AwsConfig(
    AppConfig.amazonKey,
    AppConfig.amazonSecret,
    AppConfig.assetsBucket)

  lazy val archiveConfig = ArchiveConfig(
    new ObjectId(config.getString("archive.contentCollectionId").getOrElse("?")),
    new ObjectId(config.getString("archive.orgId").getOrElse("?")))

  lazy val s3: AmazonS3 = new AmazonS3Client(new AWSCredentials {
    override lazy val getAWSAccessKeyId: String = aws.key

    override lazy val getAWSSecretKey: String = aws.secret
  })

  lazy val accessTokenConfig = AccessTokenConfig()

  def controllers: Seq[Controller] = Seq(itemDrafts)

  override lazy val db: MongoDB = Db.salatDb()

  @deprecated("This is a legacy function - remove", "1.0")
  override lazy val mode: AppMode = AppMode(Play.current.mode.toString.toLowerCase())

  override lazy val context: Context = new ServicesContext(Play.classloader)

  override lazy val identifyUser: (RequestHeader) => Option[OrgAndUser] = ???

  override lazy val jsonFormatting: JsonFormatting = new JsonFormatting {
    override lazy val findStandardByDotNotation: (String) => Option[Standard] = standard.findOneByDotNotation(_)

    override lazy val fieldValue: FieldValue = Main.this.fieldValue.get.get

    override lazy val findSubjectById: (ObjectId) => Option[Subject] = subject.findOneById(_)

    override lazy val rootOrgId: ObjectId = AppConfig.rootOrgId
  }

  def initServiceLookup() = {
    ServiceLookup.apiClientService = apiClient
    ServiceLookup.contentCollection = contentCollection
    ServiceLookup.itemService = item
    ServiceLookup.jsonFormatting = jsonFormatting
    ServiceLookup.orgService = org
    ServiceLookup.registrationTokenService = registrationToken
    ServiceLookup.userService = user
  }

}