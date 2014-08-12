package org.corespring.platform.core.models.item

import play.api.Logger
import play.api.cache.Cache
import play.api.libs.json.JsValue

trait ItemTransformationCache {

  def getCachedTransformation(item: Item): Option[JsValue]

  def setCachedTransformation(item: Item, transformation: JsValue): Unit

  def removeCachedTransformation(item: Item): Unit
}

class PlayItemTransformationCache extends ItemTransformationCache {

  import play.api.Play.current

  private def transformKey(item: Item) = s"qti_transformation_${item.id}"

  def getCachedTransformation(item: Item): Option[JsValue] = Cache.get(transformKey(item)) match {
    case Some(json: JsValue) => Some(json)
    case Some(_) => {
      Logger.debug(s"Invalid transformation serialization in cache for item ${item.id}")
      None
    }
    case _ => None
  }

  def setCachedTransformation(item: Item, transformation: JsValue) = {
    Logger.debug(s"Adding cached transformation for ${item.id}")
    Cache.set(transformKey(item), transformation)
  }

  def removeCachedTransformation(item: Item) = {
    Logger.debug(s"Removing cached transformation for ${item.id}")
    Cache.remove(transformKey(item))
  }

}

object PlayItemTransformationCache extends PlayItemTransformationCache()
