package org.corespring.importing

import org.corespring.common.config.AppConfig
import org.corespring.importing.controllers.ItemImportController
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.mvc.Controller

class Bootstrap(itemAuth: ItemAuth[OrgAndOpts],
                appConfig: AppConfig) {

  import appConfig._

  protected val itemFileConverter = new ItemFileConverter {
    override def uploader = new TransferManagerUploader(amazonKey, amazonSecret, assetsBucket)
    override def itemService = ItemServiceWired
  }

  lazy val itemImportController = new ItemImportController(itemFileConverter)

  lazy val controllers: Seq[Controller] = Seq(itemImportController)

}
