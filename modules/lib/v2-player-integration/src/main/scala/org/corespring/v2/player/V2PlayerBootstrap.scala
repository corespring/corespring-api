package org.corespring.v2.player

import java.io.File

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.{ FileUtils, IOUtils }
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client._
import org.corespring.container.client.controllers.ComponentSets
import org.corespring.container.client.hooks.{ EditorHooks => ContainerEditorHooks, DraftHooks => ContainerDraftHooks, CollectionHooks => ContainerCollectionHooks, CoreItemHooks, CreateItemHook, DataQueryHooks }
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import org.corespring.drafts.item.{ S3Paths, ItemDrafts }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.item.{ FieldValueService, ItemService }
import org.corespring.services._
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers._
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.Logger
import org.corespring.v2.player.hooks._
import org.corespring.v2.player.{ hooks => apiHooks }
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.{ JsArray, JsObject, JsValue, Json }
import play.api.mvc._
import play.api.mvc.Results._
import play.api.{ Configuration, Mode => PlayMode, Play }
import org.apache.commons.httpclient.util.URIUtil

import scala.concurrent.ExecutionContext
import scalaz.{ Validation }

trait V2ApiServices {
  def orgService: OrganizationService
  def contentCollectionService: ContentCollectionService
  def fieldValue: FieldValueService
  def itemService: ItemService
  def itemDrafts: ItemDrafts
  def itemAuth: ItemAuth[OrgAndOpts]
  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]
  def standardService: StandardService
  def subjectService: SubjectService
  def s3Service: S3Service
  def userService: UserService
}

class V2PlayerBootstrap(
  val services: V2ApiServices,
  val components: Seq[Component],
  val configuration: Configuration,
  resolveDomainPaths: String => String,
  itemTransformer: ItemTransformer,
  identifier: RequestIdentity[OrgAndOpts],
  bucket: String,
  getItemIdForSessionId: String => Option[VersionedId[ObjectId]],
  jsonFormatting: JsonFormatting)

  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger(classOf[V2PlayerBootstrap])

  override def versionInfo: JsObject = VersionInfo(configuration)

  def ec: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.item-session-api")

  override def resolveDomain(path: String) = resolveDomainPaths(path)

  override def componentSets: ComponentSets = new CompressedAndMinifiedComponentSets {

    import play.api.Play.current

    override def allComponents: Seq[Component] = V2PlayerBootstrap.this.components

    override def configuration = {
      val rc = V2PlayerBootstrap.this.configuration
      val c = ConfigFactory.parseString(
        s"""
             |minify: ${rc.getBoolean("components.minify").getOrElse(Play.mode == PlayMode.Prod)}
             |gzip: ${rc.getBoolean("components.gzip").getOrElse(Play.mode == PlayMode.Prod)}
           """.stripMargin)

      new Configuration(c)
    }

    override def dependencyResolver: DependencyResolver = new DependencyResolver {
      override def components: Seq[Component] = V2PlayerBootstrap.this.components
    }

    override def resource(path: String): Option[String] = Play.resource(s"container-client/bower_components/$path").map { url =>
      logger.trace(s"load resource $path")
      val input = url.openStream()
      val content = IOUtils.toString(input, "UTF-8")
      IOUtils.closeQuietly(input)
      content
    }

    override def loadLibrarySource(path: String): Option[String] = {
      val componentsPath = V2PlayerBootstrap.this.configuration.getString("components.path").getOrElse("?")
      val fullPath = s"$componentsPath/$path"
      val file = new File(fullPath)

      if (file.exists()) {
        logger.trace(s"load file: $path")
        Some(FileUtils.readFileToString(file, "UTF-8"))
      } else {
        Some(s"console.warn('failed to log $fullPath');")
      }
    }
  }

  override def dataQueryHooks: DataQueryHooks = new apiHooks.DataQueryHooks {
    override def subjectQueryService: QueryService[Subject] = services.subjectService //SubjectQueryService

    override def standardQueryService: QueryService[Standard] = services.standardService

    override val fieldValueJson: JsObject = {
      services.fieldValue.get.map { fv =>

        implicit val fvFormat = jsonFormatting.writesFieldValue

        Json.toJson(fv).as[JsObject]
      }.getOrElse {
        throw new RuntimeException("Can't load field values")
      }
    }

    override val standardsTreeJson: JsArray = {
      import play.api.Play.current
      Play.resourceAsStream("public/web/standards_tree.json").map { is =>
        val contents = IOUtils.toString(is, "UTF-8")
        IOUtils.closeQuietly(is)
        Json.parse(contents).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }

    override def jsonFormatting: JsonFormatting = V2PlayerBootstrap.this.jsonFormatting
  }

  trait WithDefaults extends HasContext {
    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = identifier(request)
    def transform: (Item) => JsValue = itemTransformer.transformToV2Json
    def transformItem: (Item) => JsValue = transform
    implicit override def ec: ExecutionContext = V2PlayerBootstrap.this.ec
    def itemService: ItemService = services.itemService
  }

  override def sessionHooks: SessionHooks = new apiHooks.SessionHooks with WithDefaults {
    override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = services.sessionAuth

    override def jsonFormatting: JsonFormatting = V2PlayerBootstrap.this.jsonFormatting
  }

  override def itemHooks: CoreItemHooks with CreateItemHook = new apiHooks.ItemHooks with WithDefaults {
    override def auth: ItemAuth[OrgAndOpts] = services.itemAuth
  }

  override def itemDraftHooks: ItemDraftHooks = new ItemDraftHooks with WithDefaults {
    override def backend: ItemDrafts = services.itemDrafts

    override def orgService: OrganizationService = services.orgService

    override def jsonFormatting: JsonFormatting = V2PlayerBootstrap.this.jsonFormatting
  }

  override def playerLauncherHooks: PlayerLauncherHooks = new apiHooks.PlayerLauncherHooks with WithDefaults {
    override def userService: UserService = services.userService
  }

  override def collectionHooks: ContainerCollectionHooks = new apiHooks.CollectionHooks with WithDefaults {
    override def colService: ContentCollectionService = services.contentCollectionService
  }

  /**
   * NOTE: This should only be a temporary solution--we should run a migration that either sets all of our AWS keys to
   * be URI encoded or not URI encoded.
   */
  private def getAssetFromItemId(s3Path: String): SimpleResult = {
    val result = services.s3Service.download(bucket, URIUtil.decode(s3Path))
    val isOk = result.header.status / 100 == 2
    if (isOk) result else services.s3Service.download(bucket, s3Path)
  }

  private def versionedIdFromString(itemService: ItemService, id: String): Option[VersionedId[ObjectId]] = {
    VersionedId(id).map { vid =>
      val version = vid.version.getOrElse(itemService.currentVersion(vid))
      VersionedId(vid.id, Some(version))
    }
  }

  override def catalogHooks: CatalogHooks = new apiHooks.CatalogHooks with WithDefaults {
    override def auth: ItemAuth[OrgAndOpts] = services.itemAuth

    override def loadFile(id: String, path: String)(request: Request[AnyContent]) =
      versionedIdFromString(itemService, id).map { vid =>
        getAssetFromItemId(S3Paths.itemFile(vid, path))
      }.getOrElse(BadRequest(s"Invalid versioned id: $id"))

    override def loadSupportingMaterialFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
      versionedIdFromString(itemService, id).map { vid =>
        getAssetFromItemId(S3Paths.itemSupportingMaterialFile(vid, path))
      }.getOrElse(BadRequest(s"Invalid versioned id: $id"))
    }
  }

  override def playerHooks: PlayerHooks = new apiHooks.PlayerHooks with WithDefaults {
    override def itemTransformer = V2PlayerBootstrap.this.itemTransformer
    override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = services.sessionAuth

    override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = {
      versionedIdFromString(itemService, itemId).map { vid =>
        getAssetFromItemId(S3Paths.itemFile(vid, file))
      }.getOrElse(BadRequest(s"Invalid versioned id: $itemId"))
    }

    override def loadFile(id: String, path: String)(request: Request[AnyContent]) =
      getItemIdForSessionId(id).map { vid =>
        require(vid.version.isDefined, s"The version must be defined: $vid")
        getAssetFromItemId(S3Paths.itemFile(vid, path))
      }.getOrElse(NotFound(s"Can't find an item id for session: $id"))

    override def jsonFormatting: JsonFormatting = V2PlayerBootstrap.this.jsonFormatting
  }

  override def draftEditorHooks: ContainerEditorHooks = new apiHooks.DraftEditorHooks with WithDefaults {

    override def playS3: S3Service = services.s3Service

    override def bucket: String = V2PlayerBootstrap.this.bucket

    override def backend: ItemDrafts = services.itemDrafts
  }

  override def itemEditorHooks: ContainerEditorHooks = new apiHooks.ItemEditorHooks with WithDefaults {

    override def playS3: S3Service = services.s3Service

    override def bucket: String = V2PlayerBootstrap.this.bucket

    override def itemAuth: ItemAuth[OrgAndOpts] = services.itemAuth

    override def itemService: ItemService = services.itemService
  }

}
