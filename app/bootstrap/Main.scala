package bootstrap

import bootstrap.Actors.UpdateItem
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client, S3ClientOptions }
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import common.db.Db
import developer.ServiceLookup
import org.apache.commons.io.IOUtils
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.assets.CorespringS3ServiceExtended
import org.corespring.common.config.AppConfig
import org.corespring.container.client.integration
import org.corespring.container.client.integration.{ DefaultIntegration, ContainerControllers }
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.drafts.item.models.{ OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.encryption.EncryptionModule
import org.corespring.itemSearch.{ ElasticSearchExecutionContext, ElasticSearchUrl, ItemSearchModule }
import org.corespring.models.appConfig.{ AccessTokenConfig, ArchiveConfig, Bucket }
import org.corespring.models.item.{ ComponentType, FieldValue }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.{ ItemTransformer, ItemTransformerConfig }
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap._
import org.corespring.v2.api.services.{ BasicScoreService, ScoreService }
import org.corespring.v2.api.{ ItemApiExecutionContext, ItemSessionApiExecutionContext, V2ApiModule }
import org.corespring.v2.auth.V2AuthModule
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.hooks.{ StandardsTree }
import org.corespring.v2.player.{ AllItemVersionTransformer, TransformerItemService, V2PlayerModule }
import org.corespring.v2.sessiondb._
import org.corespring.web.user.SecureSocial
import play.api.Mode.{ Mode => PlayMode }
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc._
import play.api.{ Configuration, Logger, Play }

import scala.concurrent.ExecutionContext
import scalaz.Validation

object Main
  extends SalatServices
  with EncryptionModule
  with ItemSearchModule
  with V2AuthModule
  with V2ApiModule
  with V2PlayerModule
  with SessionDbModule {

  import com.softwaremill.macwire.MacwireMacros._
  import play.api.Play.current

  private lazy val logger = Logger(Main.getClass)

  logger.debug("bootstrapping...")

  override lazy val controllers: Seq[Controller] = {
    Seq(itemDraftsController) ++ super.controllers ++ v2ApiControllers
  }

  lazy val configuration = current.configuration

  override lazy val elasticSearchUrl: ElasticSearchUrl = ElasticSearchUrl(AppConfig.elasticSearchUrl)

  override lazy val elasticSearchExecutionContext: ElasticSearchExecutionContext = ElasticSearchExecutionContext(ExecutionContext.Implicits.global)

  lazy val transformerItemService = new TransformerItemService(itemService,
    db("versioned_content"),
    db("content"))(context)

  lazy val itemTransformerConfig = ItemTransformerConfig(
    configuration.getBoolean("v2.itemTransformer.checkModelIsUpToDate").getOrElse(false))

  override def sessionDBConfig: SessionDbConfig = SessionDbConfig(AppConfig.dynamoDbActivate)

  override lazy val awsCredentials: AWSCredentials = new AWSCredentials {
    override lazy val getAWSAccessKeyId: String = AppConfig.amazonKey
    override lazy val getAWSSecretKey: String = AppConfig.amazonSecret
  }

  override lazy val itemTransformer: ItemTransformer = wire[AllItemVersionTransformer]

  override lazy val sessionCreatedCallback: VersionedId[ObjectId] => Unit = {
    (itemId) =>
      Actors.itemTransformerActor ! UpdateItem(itemId)
  }

  override lazy val componentTypes: Seq[ComponentType] = componentLoader.all.map {
    c => ComponentType(c.componentType, (c.packageInfo \ "title").asOpt[String].getOrElse(c.componentType))
  }

  override lazy val itemSessionApiExecutionContext: ItemSessionApiExecutionContext = ItemSessionApiExecutionContext(ExecutionContext.Implicits.global)

  private lazy val secureSocial: SecureSocial = new SecureSocial {}

  private lazy val requestIdentifiers: RequestIdentifiers = wire[RequestIdentifiers]

  override lazy val getOrgAndOptsFn: (RequestHeader) => Validation[V2Error, OrgAndOpts] = requestIdentifiers.allIdentifiers.apply

  override lazy val itemApiExecutionContext: ItemApiExecutionContext = ItemApiExecutionContext(ExecutionContext.global)

  override lazy val scoreService: ScoreService = new BasicScoreService(outcomeProcessor, scoreProcessor)(jsonFormatting.formatPlayerDefinition)

  /**
   * Note: macwire > 1.0 has a tagging option so that you can tag instances of the same type
   * in a scope so it knows which property to inject (eg if we have multiple strings in scope)
   * However this won't be available until we move to scala 2.11 and will probably involve a bump of play too.
   * In the interim we create simple types to wrap the strings
   */
  override lazy val bucket = Bucket(AppConfig.assetsBucket)

  override lazy val archiveConfig = ArchiveConfig(AppConfig.archiveContentCollectionId, AppConfig.archiveOrgId)

  override lazy val accessTokenConfig = AccessTokenConfig()

  override lazy val s3: AmazonS3 = {
    val client = new AmazonS3Client(awsCredentials)

    AppConfig.amazonEndpoint.foreach { e =>
      client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))
      client.setEndpoint(e)
    }
    client
  }

  override lazy val db: MongoDB = Db.salatDb()

  override lazy val context: Context = new ServicesContext(Play.classloader)

  override lazy val identifyUser: RequestHeader => Option[OrgAndUser] = (rh) => {

    def orgAndOptsToOrgAndUser(o: OrgAndOpts): OrgAndUser = OrgAndUser(
      SimpleOrg.fromOrganization(o.org),
      o.user.map(SimpleUser.fromUser))

    getOrgAndOptsFn.apply(rh).map(o => orgAndOptsToOrgAndUser(o)).toOption
  }

  override lazy val jsonFormatting: JsonFormatting = new JsonFormatting {
    override lazy val findStandardByDotNotation: (String) => Option[Standard] = standardService.findOneByDotNotation(_)

    override lazy val fieldValue: FieldValue = fieldValueService.get.get

    override lazy val findSubjectById: (ObjectId) => Option[Subject] = subjectService.findOneById(_)

    override lazy val rootOrgId: ObjectId = AppConfig.rootOrgId
  }

  def initServiceLookup() = {
    ServiceLookup.apiClientService = apiClientService
    ServiceLookup.contentCollectionService = contentCollectionService
    ServiceLookup.itemService = itemService
    ServiceLookup.jsonFormatting = jsonFormatting
    ServiceLookup.orgService = orgService
    ServiceLookup.registrationTokenService = registrationTokenService
    ServiceLookup.userService = userService
  }

  override def s3Service: S3Service = wire[CorespringS3ServiceExtended]

  lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.getString("components.path").toSeq

    val showNonReleasedComponents: Boolean = containerConfig.getBoolean("components.showNonReleasedComponents")
      .getOrElse {
        Play.current.mode == play.api.Mode.Dev
      }

    val out = new FileComponentLoader(path, showNonReleasedComponents)
    out.reload
    out
  }

  private lazy val containerConfig = {
    for {
      container <- current.configuration.getConfig("container")
      modeSpecific <- current.configuration
        .getConfig(s"container-${Play.current.mode.toString.toLowerCase}")
        .orElse(Some(Configuration.empty))
    } yield {
      val out = container ++ modeSpecific ++ current.configuration
        .getConfig("v2.auth")
        .getOrElse(Configuration.empty)
      logger.debug(s"Container config: \n${out.underlying.root.render}")
      out
    }
  }.getOrElse(Configuration.empty)

  override lazy val standardTree: StandardsTree = {
    val json: JsArray = {
      import play.api.Play.current
      Play.resourceAsStream("public/web/standards_tree.json").map { is =>
        val contents = IOUtils.toString(is, "UTF-8")
        IOUtils.closeQuietly(is)
        Json.parse(contents).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }
    StandardsTree(json)
  }

  override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

  override def playMode: PlayMode = Play.current.mode

}