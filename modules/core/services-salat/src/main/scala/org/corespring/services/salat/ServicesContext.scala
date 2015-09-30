package org.corespring.services.salat

import org.corespring.salat.config.SalatContext
import org.corespring.services.salat.item.{ FileTransformer, PlayerDefinitionTransformer }

class ServicesContext(classLoader: ClassLoader)
  extends SalatContext(classLoader) {
  registerCustomTransformer(new PlayerDefinitionTransformer(this))
  registerCustomTransformer(new FileTransformer(this))
  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()
}
