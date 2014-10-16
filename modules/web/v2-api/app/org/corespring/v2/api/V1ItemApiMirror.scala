package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.{ Action, AnyContent, Controller }

trait V1ItemApiMirror extends Controller {

  def get: (VersionedId[ObjectId], Option[String]) => Action[AnyContent]

  def list: (Option[String], Option[String], String, Int, Int, Option[String]) => Action[AnyContent]

  def listWithColl: (ObjectId, Option[String], Option[String], String, Int, Int, Option[String]) => Action[AnyContent]
}
