package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.{ Action, AnyContent, Controller }

trait V1ItemSessionApiMirror extends Controller {

  def reopen: (VersionedId[ObjectId], ObjectId) => Action[AnyContent]
}

