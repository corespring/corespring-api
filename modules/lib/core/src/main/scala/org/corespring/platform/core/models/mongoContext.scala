package org.corespring.platform.core.models

import com.novus.salat.{ TypeHintFrequency, StringTypeHintStrategy, Context }
import play.api.Play
import play.api.Play.current

object mongoContext {
  implicit val context = {
    val context = new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }

    context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
    context.registerClassLoader(Play.classloader)
    context
  }
}