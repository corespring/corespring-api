package org.corespring.container.client.controllers.apps

import play.api.libs.json.Json
import v2Player.Routes

object StaticPaths {
  val assetUrl = Routes.prefix + "/images"

  val staticPaths = Json.obj(
    "assets" -> assetUrl,
    "dataQuery" -> org.corespring.container.client.controllers.routes.DataQuery.list(":topic").url.replace("/:topic", ""),
    "collection" -> org.corespring.container.client.controllers.resources.routes.Collection.list().url,
    "metadata" -> org.corespring.container.client.controllers.resources.routes.ItemMetadata.get(":id").url)
}
