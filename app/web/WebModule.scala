package web

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.itemSearch.AggregateType.{ ItemType, WidgetType }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.json.JsonFormatting
import org.corespring.services.item.{ FieldValueService, ItemService }
import org.corespring.services.{ OrganizationService, UserService }
import play.api.Mode.Mode
import web.controllers.{ Partials, PublicSite }
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import web.controllers.{ Main, ShowResource }
import web.models.{ ContainerVersion, WebExecutionContext }

case class PublicSiteConfig(url: String)
case class DefaultOrgs(v2Player: Seq[ObjectId], root: ObjectId)

trait WebModule {

  def itemService: ItemService
  def playerTokenService: PlayerTokenService
  def s3Service: S3Service
  def fieldValueService: FieldValueService
  def jsonFormatting: JsonFormatting
  def userService: UserService
  def orgService: OrganizationService
  def itemType: ItemType
  def widgetType: WidgetType
  def containerVersion: ContainerVersion
  def webExecutionContext: WebExecutionContext
  def mode: Mode
  def defaultOrgs: DefaultOrgs
  def bucket: Bucket
  def publicSiteConfig: PublicSiteConfig
  def userSessionOrgIdentity: UserSessionOrgIdentity[OrgAndOpts]

  lazy val showResource = new ShowResource(itemService, s3Service, bucket)
  lazy val partials = new Partials(mode, defaultOrgs)
  lazy val webMain = new Main(
    fieldValueService,
    jsonFormatting,
    userService,
    orgService,
    itemType,
    widgetType,
    containerVersion,
    webExecutionContext,
    playerTokenService,
    userSessionOrgIdentity)

  lazy val publicSite = new PublicSite(publicSiteConfig.url, mode)

  lazy val webControllers = Seq(showResource, webMain, publicSite, partials)
}
