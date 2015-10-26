package org.corespring.salat.config

import com.novus.salat.{ TypeHintFrequency, StringTypeHintStrategy, Context }

class SalatContext(classLoader: ClassLoader)
  extends Context {
  val name = "global"
  override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
  registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
  registerClassLoader(classLoader)
  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()
}
