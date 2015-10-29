package web

import org.corespring.amazon.s3.S3Service
import org.corespring.itemSearch.AggregateType.{ WidgetType, ItemType }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.services.item.{ FieldValueService, ItemService }
import web.controllers.{ Main, ShowResource }

trait WebModule {

  def itemService: ItemService
  def s3Service: S3Service
  def fieldValueService: FieldValueService
  def jsonFormatting: JsonFormatting
  def userService: UserService
  def orgService: OrganizationService
  def itemType: ItemType
  def widgetType: WidgetType

  lazy val showResource = new ShowResource(itemService, s3Service)
  lazy val webMain = new Main(fieldValueService, jsonFormatting, userService, orgService, itemType, widgetType)

  lazy val webControllers = Seq(showResource, webMain)
}
