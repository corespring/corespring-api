package web
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.VersionInfo
import org.corespring.itemSearch.AggregateType.{ ItemType, WidgetType }
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.appConfig.{ Bucket, DefaultOrgs }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.auth.ApiClientService
import org.corespring.services.item.{ FieldValueService, ItemService }
import org.corespring.services.{ OrgCollectionService, OrganizationService, UserService }
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.web.common.controllers.deployment.AssetsLoader
import play.api.Mode.Mode
import web.controllers._
import web.models.WebExecutionContext

case class WebModuleConfig(mode: Mode)
case class PublicSiteConfig(url: String)

case class WebV2Actions(actions: V2Actions)

trait WebModule {

  def apiClientService: ApiClientService
  def webV2Actions: WebV2Actions
  def itemService: ItemService
  def playerTokenService: PlayerTokenService
  def s3Service: S3Service
  def fieldValueService: FieldValueService
  def jsonFormatting: JsonFormatting
  def userService: UserService
  def orgService: OrganizationService
  def itemType: ItemType
  def widgetType: WidgetType
  def versionInfo: VersionInfo
  lazy val containerVersionInfo: VersionInfo = versionInfo
  def webExecutionContext: WebExecutionContext
  def webModuleConfig: WebModuleConfig
  def defaultOrgs: DefaultOrgs
  def bucket: Bucket
  def publicSiteConfig: PublicSiteConfig
  def assetsLoader: AssetsLoader

  def itemIndexService: ItemIndexService
  def orgCollectionService: OrgCollectionService

  lazy val itemSearch: ItemSearch = new ItemSearch(
    webV2Actions.actions,
    itemIndexService,
    orgCollectionService,
    webExecutionContext)

  lazy val showResource = new ShowResource(itemService, s3Service, bucket)
  lazy val partials = new Partials(webModuleConfig.mode, defaultOrgs)
  lazy val webMain = new Main(
    webV2Actions.actions,
    fieldValueService,
    jsonFormatting,
    userService,
    orgService,
    itemType,
    widgetType,
    containerVersionInfo,
    webExecutionContext,
    playerTokenService,
    assetsLoader)

  lazy val publicSite = new PublicSite(publicSiteConfig.url, webModuleConfig.mode)
  lazy val systemCheck = new SystemCheck()

  lazy val webControllers = Seq(showResource, webMain, publicSite, partials, itemSearch, systemCheck)
}
