package org.corespring.v2.api

import org.bson.types.ObjectId
import play.api.mvc.{ Action, AnyContent, Controller }

trait V1CollectionApiProxy extends Controller {

  def getCollection: (ObjectId) => Action[AnyContent]

  def list: (Option[String], Option[String], String, Int, Int, Option[String]) => Action[AnyContent]

}
