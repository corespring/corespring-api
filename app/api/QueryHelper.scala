package api

import com.mongodb.casbah.commons.MongoDBObject
import play.api.mvc.Request
import play.api.Logger
import com.mongodb.util.{JSONParseException, JSON}
import com.mongodb.casbah.Imports._
import java.lang.reflect.Field

/**
 * A helper class to parse queries in urls
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
}

case class InvalidFieldException(field: String) extends RuntimeException("Unknown field %s".format(field))
