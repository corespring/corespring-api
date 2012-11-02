package api

import play.api.mvc.{Result, Results}
import play.api.Logger
import com.mongodb.util.{JSONParseException, JSON}
import play.api.libs.json.{JsNumber, JsObject, Writes, Json}
import com.novus.salat.dao.SalatDAO
import com.mongodb.casbah.Imports._
import models.{Identifiable, Queryable, DBQueryable, QueryField}
import controllers.{LogType, InternalError, Utils, QueryParser}


/**
 * A helper class to handle list queries from all the controllers
 */
object QueryHelper {

  /**
   * Helper method to execute list queries from all the controllers.
   *
   * @param q  the query
   * @param f  the fields to include/exclude
   * @param c  if set to true will return the number of entries matching instead of the entries themselves.
   * @param sk how many entries to skip
   * @param l  the maximum number of entries to return
   *
   * @return
   */
  def list[T <: Identifiable](q: Option[String], f:Option[Object], c: String, sk: Int, l: Int, queryable:DBQueryable[T], initSearch:Option[DBObject] = None)(implicit writes:Writes[T]): Result = {
    f.map(toFieldObject(_,queryable)).getOrElse(Right(new MongoDBObject())) match {
      case Right(fields) => {
        val qp:QueryParser = q.map(qstr => QueryParser.buildQuery(qstr, queryable)).getOrElse( QueryParser())
        if (qp.ignoredKeys.isEmpty){
          qp.result match {
            case Right(builder) =>
              val combinedQuery:DBObject = initSearch.map( extraParams => builder.result() ++ extraParams).getOrElse( builder.result() )
              val cursor = queryable.find(combinedQuery,fields)
              cursor.skip(sk)
              cursor.limit(l)
              if ( c.equalsIgnoreCase("true") ) {
                Results.Ok(JsObject(Seq("count" -> JsNumber(cursor.count))))
              }else {
                Results.Ok(Json.toJson(Utils.toSeq(cursor)))
              }
            case Left(e) =>
              Results.BadRequest(Json.toJson(ApiError.UnknownFieldOrOperator(e.clientOutput)))
          }
        }else Results.BadRequest(Json.toJson(ApiError.UnknownFieldOrOperator(Some("the following keys were ignored: "+qp.ignoredKeys.foldRight[String]("")((key,acc) => key+", "+acc)))))
      }
      case Left(e) =>
        Results.BadRequest(Json.toJson(ApiError.UnknownFieldOrOperator(e.clientOutput)))
    }
  }
  private def toFieldObject[T <: AnyRef](f:Object, queryable: Queryable[T]):Either[InternalError,MongoDBObject] = {
    def parseDBObject(dbo:DBObject):Either[InternalError,MongoDBObject] = dbo.iterator.find(field => !queryable.queryFields.exists(_.key == field._1) || !field._2.isInstanceOf[Int]) match {
      case Some((key,_)) => Left(InternalError("either field key was invalid or value was NAN for "+key,LogType.printError,true))
      case None => Right(MongoDBObject(dbo.iterator.toList))
    }
    f match {
      case dbo:DBObject => parseDBObject(dbo)
      case qfstr:String => JSON.parse(qfstr) match {
        case dbo:DBObject => parseDBObject(dbo)
        case _ => Left(InternalError("invalid query string",LogType.printError,true))
      }
    }
  }
}

case class InvalidFieldException(field: String) extends RuntimeException("Unknown field %s".format(field))
