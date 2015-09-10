package org.corespring.importing

import org.apache.commons.io.IOUtils
import org.corespring.common.aws.AwsUtil
import org.corespring.common.config.AppConfig
import org.corespring.importing.controllers.ItemImportController
import org.corespring.models.json.JsonFormatting
import org.corespring.services.OrganizationService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.Play
import play.api.mvc.Controller

class Bootstrap(
  itemAuth: ItemAuth[OrgAndOpts],
  userSession: UserSessionOrgIdentity[OrgAndOpts],
  orgService: OrganizationService,
  val itemService: ItemService,
  jsonFormatting: JsonFormatting,
  appConfig: AppConfig) {

  import appConfig._

  protected val itemFileConverter = new ItemFileConverter {
    override def uploader = new TransferManagerUploader(AwsUtil.credentials(), assetsBucket)
    override def itemService = Bootstrap.this.itemService
    override def jsonFormatting: JsonFormatting = Bootstrap.this.jsonFormatting

    override def readFile(path: String): Option[String] = {
      Play.current.resource(path).map { url => IOUtils.toString(url) }
    }
  }

  lazy val itemImportController = new ItemImportController(itemFileConverter, userSession, orgService)

  lazy val controllers: Seq[Controller] = Seq(itemImportController)

}
