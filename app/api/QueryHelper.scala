package api

import play.api.mvc.{Result, Results}
import play.api.Logger
import com.mongodb.util.{JSONParseException, JSON}
import play.api.libs.json.{Writes, Json}
import com.novus.salat.dao.SalatDAO
import com.mongodb.casbah.Imports._


/**
 * A helper class to handle list queries from all the controllers
 */
object QueryHelper {
  val leftSideOperators = Map (
    ("$or" ->  checkOrOperator _)
  )

  val rightSideOperators = Map(
    ("$in" -> checkInOperator _)
  )

  def parse[T](q: String, validFields: Map[String, String]):MongoDBObject = {
    Logger.debug("fields = " + validFields.mkString(","))
      val query = JSON.parse(q).asInstanceOf[DBObject]
      for ( f <- query.iterator ) {
        // check if it's a valid field
        Logger.debug("checking field: " + f._1)
        Logger.debug("         value: " + f._2)
        validFields.get(f._1) match {
          case Some(vf) => {
            Logger.debug("checking if field value = %s (class = %s) is an operator".format(vf,vf.getClass))
            // todo: add some more checking here
          }
          case _ => {
            // not a valid field, it could be an operator
            leftSideOperators.get(f._1) match {
              case Some(checkOperator) => {
                Logger.debug("checking if operator: " + f._1 + " has valid values")
                checkOperator(validFields, f._2)
              }
              case _ => {
                throw new InvalidFieldException(f._1)
              }
            }
          }
        }
      }
      query
  }

  private def checkInOperator(validFields: Map[String, String], obj: Object) {
    Logger.debug("in obj = " + obj)
    Logger.debug("in obj = " + obj.getClass)
    // todo: check values?
  }

  private def checkOrOperator(validFields: Map[String, String], obj: Object) {
    val list = obj.asInstanceOf[BasicDBList]
    val iterator = list.iterator()

    while ( iterator.hasNext ) {
      val item = iterator.next().asInstanceOf[BasicDBObject]

      for ( key <- item.keys ) {
        Logger.debug("checking if %s is a valid field".format(key))
        validFields.get(key) match {
          case Some(fieldType) => {
            // todo: check field type?
          }
          case _ => throw new InvalidFieldException(key)
        }
      }
    }
  }

  /**
   * Helper method to execute list queries from all the controllers.
   *
   * @param q  the query
   * @param f  the fields to include/exclude
   * @param c  if set to true will return the number of entries matching instead of the entries themselves.
   * @param sk how many entries to skip
   * @param l  the maximum number of entries to return
   * @param validFields the valid fields
   * @param dao the collection that needs to be queried
   *
   * @return
   */
  def list[ObjectType <: AnyRef, ID <: Any](q: Option[String], f: Option[Object], c: String, sk: Int, l: Int, validFields: Map[String, String], dao: SalatDAO[ObjectType, ID], writes: Writes[ObjectType], initSearch:Option[DBObject] = None): Result = {
    Logger.debug("QueryHelper: q = %s, f = %s, c = %s, sk = %d, l = %d".format(q, f, c, sk, l))
    try {
      val query = q.map( QueryHelper.parse(_, validFields) ).getOrElse( new MongoDBObject() )
      val fields = f.map( fieldSet => {
        if ( fieldSet.isInstanceOf[String] ) {
          JSON.parse(fieldSet.asInstanceOf[String]).asInstanceOf[DBObject]
        } else {
          fieldSet.asInstanceOf[DBObject]
        }
      })
      fields.foreach(validateFields(_, validFields))
      val combinedQuery:DBObject = initSearch.map( extraParams => query ++ extraParams).getOrElse( query )
      val cursor = fields.map( dao.find(combinedQuery, _)).getOrElse( dao.find(combinedQuery))
      cursor.skip(sk)
      cursor.limit(l)

      // I'm using a String for c because if I use a boolean I need to pass 0 or 1 from the command line for Play to parse the boolean.
      // I think using "true" or "false" is better
      if ( c.equalsIgnoreCase("true") ) {
        Results.Ok(CountResult.toJson(cursor.count))
      } else {
        implicit val w = writes
        Results.Ok(Json.toJson(cursor.toList))
      }
    } catch {
      case e: JSONParseException => Results.BadRequest(Json.toJson(ApiError.InvalidQuery))
      case ife: InvalidFieldException => Results.BadRequest(Json.toJson(ApiError.UnknownFieldOrOperator.format(ife.field)))
    }
  }

  def replaceMongoId(obj: DBObject): DBObject = {
    // replace mongo's _id to id
    val id = obj.get("_id")
    obj.removeField("_id")
    MongoDBObject("id" -> id.toString) ++ obj
  }
  /**
   * Checks if all the fields in the passed DBObject are defined in the validFields map
   *
   * @param obj
   * @param validFields
   */
  def validateFields(obj: DBObject, validFields: Map[String, String]) {
    for ( f <- obj.iterator ) {
      if ( !validFields.isDefinedAt(f._1) ) {
        throw new InvalidFieldException(f._1)
      }
    }
  }
}

case class InvalidFieldException(field: String) extends RuntimeException("Unknown field %s".format(field))
