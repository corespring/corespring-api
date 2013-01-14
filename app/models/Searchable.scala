package models

import com.mongodb.{BasicDBObject, DBObject}
import com.mongodb.casbah.commons.MongoDBObject

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 1/14/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
trait Searchable {
  def toSearchObj(strjson: AnyRef): Either[InternalError,MongoDBObject]

}
