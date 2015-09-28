package org.corespring.services.salat

import org.corespring.salat.config.SalatContext
import org.corespring.services.salat.item.PlayerDefinitionTransformer

class ServicesContext(classLoader: ClassLoader)
  extends SalatContext(classLoader) {
  registerCustomTransformer(new PlayerDefinitionTransformer(this))
  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()
}
