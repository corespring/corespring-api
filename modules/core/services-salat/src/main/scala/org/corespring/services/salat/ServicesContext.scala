package org.corespring.services.salat

import org.corespring.salat.config.SalatContext
import org.corespring.services.salat.item.{ FileTransformer, PlayerDefinitionTransformer }

class ServicesContext(classLoader: ClassLoader)
  extends SalatContext(classLoader) {

  val fileTransformer = new FileTransformer(this)
  val playerDefinitionTransformer = new PlayerDefinitionTransformer(fileTransformer)
  registerCustomTransformer(fileTransformer)
  registerCustomTransformer(playerDefinitionTransformer)
  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()
}
