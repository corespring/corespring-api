package org.corespring.services.salat

import com.novus.salat.{ TypeHintFrequency, StringTypeHintStrategy, Context }
import org.corespring.services.salat.item.PlayerDefinitionTransformer

class ServicesContext(classLoader: ClassLoader) extends Context {
  val name = "global"
  override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
  registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
  registerClassLoader(classLoader)
  //registerCustomTransformer(new VersionedIdTransformer[ObjectId]())
  registerCustomTransformer(new PlayerDefinitionTransformer(this))
}
