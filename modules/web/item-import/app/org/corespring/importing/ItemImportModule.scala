package org.corespring.importing

import com.amazonaws.services.s3.transfer.TransferManager
import org.corespring.importing.controllers.ItemImportController
import org.corespring.importing.validation.{ItemSchema, ItemJsonValidator}
import org.corespring.models.appConfig.Bucket
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{OrgCollectionService}
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.mvc.Controller

trait ItemImportModule {

  import com.softwaremill.macwire.MacwireMacros._

  def transferManager : TransferManager

  def userSessionOrgIdentity: UserSessionOrgIdentity[OrgAndOpts]

  def itemService : ItemService

  def orgCollectionService : OrgCollectionService

  def bucket : Bucket

  def jsonFormatting : JsonFormatting

  def importingExecutionContext : ImportingExecutionContext

  def itemSchema : ItemSchema

  lazy val itemJsonValidator : ItemJsonValidator = wire[ItemJsonValidator]

  lazy val transferManagerUploader = wire[TransferManagerUploader]

  lazy val itemImportController : Controller = wire[ItemImportController]

  lazy val itemFileConverter : ItemFileConverter = wire[ItemFileConverter]

  lazy val itemImportControllers : Seq[Controller] = Seq(itemImportController)
}
