package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.Controller

class ItemApi(v2ItemApi: org.corespring.v2.api.ItemApi) extends Controller {

  def get(itemId: VersionedId[ObjectId], detail: Option[String] = None) = v2ItemApi.get(itemId.toString, detail)

  def countSessions(itemId: VersionedId[ObjectId]) = v2ItemApi.legacyCountSessions(itemId)
}
