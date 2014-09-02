package org.corespring.platform.core.models.item

import play.api.Logger
import play.api.cache.Cache
import play.api.libs.json.{ Json, JsValue }

trait ItemTransformationCache {

  def getCachedTransformation(item: Item): Option[JsValue]

  def setCachedTransformation(item: Item, transformation: JsValue): Unit

  def removeCachedTransformation(item: Item): Unit
}

class PlayItemTransformationCache extends ItemTransformationCache {

  import play.api.Play.current

  private lazy val logger = Logger(this.getClass.getName)

  private def transformKey(item: Item) = s"qti_transformation_${item.id}"

  def getCachedTransformation(item: Item): Option[JsValue] = {
    logger.debug(s"itemId=${item.id} function=getCachedTransformation")

    Cache.get(transformKey(item)) match {
      case Some(json: JsValue) => {
        logger.trace(s"itemId=${item.id} function=getCachedTransformation cachedJson=${Json.stringify(json)}")
        Some(json)
      }
      case Some(_) => {
        logger.warn(s"itemId=${item.id} function=getCachedTransformation - Invalid transformation serialization in cache")
        None
      }
      case _ => {
        logger.trace(s"itemId=${item.id} function=getCachedTransformation nothing in the cache")
        None
      }
    }
  }

  def setCachedTransformation(item: Item, transformation: JsValue) = {
    logger.debug(s"itemId=${item.id} function=setCachedTransformation")
    logger.trace(s"itemId=${item.id} function=setCachedTransformation json=${Json.stringify(transformation)}")
    Cache.set(transformKey(item), transformation)
  }

  def removeCachedTransformation(item: Item) = {
    logger.debug(s"itemId=${item.id} function=removeCachedTransformation")
    Cache.remove(transformKey(item))
  }

}

object PlayItemTransformationCache extends PlayItemTransformationCache()
