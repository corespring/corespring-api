package org.corespring.services.salat

import org.corespring.platform.core.models.hasVersionedIdTransformer
import org.corespring.salat.config.SalatContext
import org.corespring.services.salat.item.{ FileTransformer, PlayerDefinitionTransformer }

class ServicesContext(classLoader: ClassLoader)
  extends SalatContext(classLoader) with hasVersionedIdTransformer {

  val fileTransformer = new FileTransformer()
  val playerDefinitionTransformer = new PlayerDefinitionTransformer(fileTransformer)
  registerCustomTransformer(fileTransformer)
  registerCustomTransformer(playerDefinitionTransformer)
  registerCustomTransformer(versionedIdTransformer)
  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()
}
