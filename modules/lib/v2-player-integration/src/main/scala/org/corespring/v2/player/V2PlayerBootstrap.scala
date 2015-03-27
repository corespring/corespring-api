package org.corespring.v2.player

import java.io.File

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.{ FileUtils, IOUtils }
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client._
import org.corespring.container.client.controllers.ComponentSets
import org.corespring.container.client.hooks.{ DataQueryHooks, EditorHooks => ContainerEditorHooks, ItemHooks => ContainerItemHooks }
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import org.corespring.drafts.item.ItemDrafts
import org.corespring.platform.core.models.item.{ FieldValue, Item, PlayerDefinition }
import org.corespring.platform.core.models.{ Standard, Subject }
import org.corespring.platform.core.services._
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers._
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.v2.player.hooks._
import org.corespring.v2.player.{ hooks => apiHooks }
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.{ JsArray, JsObject, JsValue, Json }
import play.api.mvc._
import play.api.{ Configuration, Mode => PlayMode, Play }

import scala.concurrent.ExecutionContext
import scalaz.Validation

class V2PlayerBootstrap(
  val components: Seq[Component],
  val configuration: Configuration,
  val resolveDomain: String => String,
  itemTransformer: ItemTransformer,
  identifier: RequestIdentity[OrgAndOpts],
  itemAuth: ItemAuth[OrgAndOpts],
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  playS3: S3Service,
  bucket: String,
  itemDrafts: ItemDrafts)

  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = V2LoggerFactory.getLogger("V2PlayerBootstrap")

  override def versionInfo: JsObject = VersionInfo(configuration)

  def ec: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.item-session-api")

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
    override def subjectQueryService: QueryService[Subject] = SubjectQueryService

    override def standardQueryService: QueryService[Standard] = StandardQueryService

    override val fieldValueJson: JsObject = {
      val dbo = FieldValue.collection.find().toSeq.head
      import com.mongodb.util.{ JSON => MongoJson }
      import play.api.libs.json.{ Json => PlayJson }
      PlayJson.parse(MongoJson.serialize(dbo)).as[JsObject]
    }

    override val standardsTreeJson: JsArray = {
      import play.api.Play.current
      Play.resourceAsStream("public/web/standards_tree.json").map { is =>
        val contents = IOUtils.toString(is, "UTF-8")
        IOUtils.closeQuietly(is)
        Json.parse(contents).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }
  }

  trait WithDefaults extends HasContext {
    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = identifier(request)
    def transform: (Item) => JsValue = itemTransformer.transformToV2Json
    def transformItem: (Item) => JsValue = transform
    implicit override def ec: ExecutionContext = V2PlayerBootstrap.this.ec
    def itemService: ItemService = ItemServiceWired
  }

  override def sessionHooks: SessionHooks = new apiHooks.SessionHooks with WithDefaults {
    override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = V2PlayerBootstrap.this.sessionAuth
  }

  override def itemHooks: ContainerItemHooks = new apiHooks.ItemHooks with WithDefaults {
    override def auth: ItemAuth[OrgAndOpts] = V2PlayerBootstrap.this.itemAuth
  }

  override def itemDraftHooks: ItemDraftHooks = new ItemDraftHooks with WithDefaults {
    override def backend: ItemDrafts = V2PlayerBootstrap.this.itemDrafts
  }

  override def playerLauncherHooks: PlayerLauncherHooks = new apiHooks.PlayerLauncherHooks with WithDefaults {
    override def userService: UserService = UserServiceWired
  }

  override def catalogHooks: CatalogHooks = new apiHooks.CatalogHooks with WithDefaults {

    override def auth: ItemAuth[OrgAndOpts] = V2PlayerBootstrap.this.itemAuth

    override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
      playS3.download(bucket, s"items/$id/$path")
    }
  }

  override def playerHooks: PlayerHooks = new apiHooks.PlayerHooks with WithDefaults {

    override def itemTransformer = V2PlayerBootstrap.this.itemTransformer

    override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = V2PlayerBootstrap.this.sessionAuth

    override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
      playS3.download(bucket, s"items/$id/$path")
    }
  }

  override def editorHooks: ContainerEditorHooks = new apiHooks.DraftEditorHooks with WithDefaults {

    override def playS3: S3Service = V2PlayerBootstrap.this.playS3

    override def bucket: String = V2PlayerBootstrap.this.bucket

    override def backend: ItemDrafts = V2PlayerBootstrap.this.itemDrafts
  }

}
