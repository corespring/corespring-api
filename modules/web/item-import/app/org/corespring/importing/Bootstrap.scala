package org.corespring.importing

import org.corespring.common.aws.AwsUtil
import org.corespring.common.config.AppConfig
import org.corespring.importing.controllers.ItemImportController
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.identifiers.{ UserSessionOrgIdentity, RequestIdentity }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.OrgService
import play.api.mvc.{ RequestHeader, Controller }

class Bootstrap(itemAuth: ItemAuth[OrgAndOpts],
  userSession: UserSessionOrgIdentity[OrgAndOpts],
  orgService: OrgService,
  appConfig: AppConfig) {

  import appConfig._

  protected val itemFileConverter = new ItemFileConverter {
    override def uploader = new TransferManagerUploader(AwsUtil.credentials(), assetsBucket)
    override def itemService = ItemServiceWired
  }

  lazy val itemImportController = new ItemImportController(itemFileConverter, userSession, orgService)

  lazy val controllers: Seq[Controller] = Seq(itemImportController)

}
