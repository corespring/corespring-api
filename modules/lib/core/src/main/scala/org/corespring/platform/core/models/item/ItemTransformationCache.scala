package org.corespring.platform.core.models.item

import play.api.cache.Cache
import play.api.libs.json.JsValue
import scala.xml.Node
import play.api.Logger

trait ItemTransformationCache {

  def getCachedTransformation(item: Item): Option[(Node, JsValue)]

  def setCachedTransformation(item: Item, transformation: (Node, JsValue)): Unit

  def removeCachedTransformation(item: Item): Unit
}

class PlayItemTransformationCache extends ItemTransformationCache {

  import play.api.Play.current

  private def transformKey(item: Item) = s"qti_transformation_${item.id}"

  def getCachedTransformation(item: Item): Option[(Node, JsValue)] = Cache.get(transformKey(item)) match {
    case Some((node: Node, json: JsValue)) => Some(node, json)
    case Some(_) => {
      Logger.debug(s"Invalid transformation serialization in cache for item ${item.id}")
      None
    }
    case _ => None
  }

  def setCachedTransformation(item: Item, transformation: (Node, JsValue)) = {
    Logger.debug(s"Adding cached transformation for ${item.id}")
    Cache.set(transformKey(item), transformation)
  }

  def removeCachedTransformation(item: Item) = {
    Logger.debug(s"Removing cached transformation for ${item.id}")
    Cache.remove(transformKey(item))
  }

}
