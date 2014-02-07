package org.corespring.platform.core.models

import com.novus.salat.{ TypeHintFrequency, StringTypeHintStrategy, Context }
import org.corespring.platform.core.models.item.PlayerDefinitionTransformer
import play.api.Play
import play.api.Play.current

object mongoContext {
  implicit val context = {
    new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
      registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
      registerClassLoader(Play.classloader)
      registerCustomTransformer(new PlayerDefinitionTransformer(this))
    }
  }

}