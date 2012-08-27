package models

import play.api.libs.json.JsValue
import com.mongodb.casbah.Imports._
import controllers.InternalError

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/27/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
trait Queryable {
  val queryFields:Map[String,String];
 // def parseQuery(json:JsValue):Either[InternalError, DBObject];
}
