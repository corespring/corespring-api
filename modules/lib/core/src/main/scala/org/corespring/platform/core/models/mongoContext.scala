package org.corespring.platform.core.models

import com.novus.salat.{ TypeHintFrequency, StringTypeHintStrategy, Context }
import org.bson.types.ObjectId
import org.corespring.models.item.PlayerDefinitionTransformer
import org.corespring.platform.data.mongo.models.VersionedIdTransformer
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
      registerCustomTransformer(new VersionedIdTransformer[ObjectId]())
      com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()
    }
  }

}