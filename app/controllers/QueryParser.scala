package controllers

import collection.mutable
import com.mongodb.casbah.Imports._
import com.mongodb.util.{JSONParseException, JSON}
import models.{DBQueryable, Queryable, QueryField}
import org.bson.types.ObjectId

case class QueryParser(var result:Either[InternalError,mutable.Builder[(String,Any),DBObject]] = Right(MongoDBObject.newBuilder), var ignoredKeys:Seq[String] = Seq()){
  override def toString() = result match {
    case Right(builder) => builder.result().toString() + "\n ignored keys: "+ignoredKeys.foldRight[String]("")((s,acc) => acc+","+s)
    case Left(e) => e.message
  }
}
object QueryParser{
  def replaceKeys(dbo:DBObject,keys:Seq[(String,String)]){
    keys.foreach(key => {
      if(dbo.contains(key._1)){
        val value = dbo.get(key._1)
        dbo.remove(key._1)
        dbo.put(key._2,value)
      }
    })
    dbo.iterator.foreach(field => {
      field._2 match {
        case dblist:BasicDBList => dblist.foreach(value => value match {
          case innerdbo:BasicDBObject => replaceKeys(innerdbo,keys)
          case _ => Log.f("invalid query")
        })
        case innerdbo:BasicDBObject => replaceKeys(innerdbo,keys)
        case _ =>
      }
    })
  }
  def removeKeys(dbo:DBObject,keys:Seq[String]){
    keys.foreach(key => dbo.remove(key))
    dbo.iterator.foreach(field => {
      field._2 match {
        case dblist:BasicDBList => dblist.foreach(value => value match {
          case innerdbo:BasicDBObject => removeKeys(innerdbo,keys)
          case _ => Log.f("invalid query")
        })
        case innerdbo:BasicDBObject => removeKeys(innerdbo,keys)
        case _ =>
      }
    })
  }
  /**
   *
   * @param query
   * @param queryable
   * @tparam T
   * @return
   */
  def buildQuery[T <: AnyRef](query:String, queryable:DBQueryable[T], queryFields:Seq[QueryField[T]] = Seq()):QueryParser = {
    try{
    JSON.parse(query) match {
      case dbo:DBObject =>
        val qp:QueryParser = queryable.preParse(dbo)
        qp.result match {
          case Right(_) => queryFields.isEmpty match {
            case false => parseOuterFields(dbo.iterator,qp,queryFields)
            case true => parseOuterFields(dbo.iterator, qp, queryable.queryFields)
          }
          case Left(e) =>  qp
        }
      case _ => QueryParser(Left(InternalError("invalid format query object",LogType.printError,true)))
    }
    }catch{
      case e:JSONParseException => QueryParser(Left(InternalError("invalid format for query",LogType.printError,true)))
    }
  }
  def buildQuery[T <: AnyRef](dbo:DBObject, qp:QueryParser, queryFields:Seq[QueryField[T]]):QueryParser = parseOuterFields(dbo.iterator, qp, queryFields)
  /**
   * recursive function that is called for each outer field in the query
   * @param iter - iterator of key-value pairs
   * @param acc - accumulator for keeping stating during recursion
   * @param includedFields - queryable object needed to check validity of fields
   * @tparam T
   * @return
   */
  private def parseOuterFields[T <: AnyRef](iter:Iterator[(String,AnyRef)], acc:QueryParser, includedFields:Seq[QueryField[T]]):QueryParser = {
    if (iter.hasNext && acc.result.isRight){
      val field = iter.next()
      parseOuterFields(iter,parseOuterField(field,acc,includedFields),includedFields)
    }else acc
  }

  /**
   * parse an individual field. if outer field key is an $and or $or then run parseOuterFields on the corresponding value
   * otherwise, run the queryable parseOuterField function
   * @param field -  the outer field
   * @param acc - accumulator used for recursion
   * @param includedFields- used for parsing of queryable fields
   * @tparam T
   * @return
   */
  private def parseOuterField[T <: AnyRef](field:(String,AnyRef), acc:QueryParser, includedFields:Seq[QueryField[T]]):QueryParser = {
    acc.result match {
      case Right(builder) => field._1 match {
        case "$and" | "$or" => field._2 match {
          case dblist:BasicDBList => if (dblist.exists(!_.isInstanceOf[BasicDBObject])){
            acc.result = Left(InternalError("element in $and array was not a object",LogType.printError,true)); acc
          }else{
            val results = dblist.map(dbo => dbo.asInstanceOf[BasicDBObject].headOption match {
              case Some(innerField) => parseOuterField(innerField,QueryParser(),includedFields)
              case None => QueryParser(Left(InternalError("empty object in "+field._1+" array",LogType.printError,true)))
            })
            results.find(_.result.isLeft) match {
              case Some(error) => acc.result = error.result; acc
              case None => {
                val value = results.filter(qp => !qp.result.right.get.result().isEmpty)
                if (!value.isEmpty){
                  value.map(qp => acc.ignoredKeys = acc.ignoredKeys ++ qp.ignoredKeys)
                  acc.result = Right(builder += (field._1 -> value.map(_.result.right.get.result())))
                }
                acc
              }
            }
          }
          case _ => acc.result = Left(InternalError("invalid value "+field._2.toString+" for "+field._1,LogType.printError,true)); acc
        }
        case _ =>
          includedFields.find(_.canHandleField(field._1)) match {
          case Some(queryField) =>
            queryField.isValueValid(field) match {
            case Right(result) => acc.result = Right(builder += result); acc
            case Left(e) => acc.result = Left(e); acc
          }
          case None => acc.ignoredKeys = acc.ignoredKeys :+ field._1; acc
        }
      }
      case Left(_) => acc
    }
  }

  private def isBaseValueValid[T <: AnyRef](field:(String,AnyRef))
                                           (validfunc:(Any) => Either[InternalError,Any]):(Either[InternalError,(String,Any)],Boolean) = {
 field._2 match {
        case dbo:BasicDBObject =>
          dbo.headOption match {
          case Some((key,value)) => key match {
            case "$in" | "$nin" => value match {
              case dblist:BasicDBList => {
                val listBuilder = MongoDBList.newBuilder
                dblist.find(v => validfunc(v) match {
                  case error:InternalError => true
                  case result => listBuilder += result; false
                }) match {
                  case Some(value) => Left(InternalError("the value "+value.toString+" was invalid for key"+key,LogType.printError)) -> true
                  case None =>
                    val test = Right(field._1 -> MongoDBObject(key -> listBuilder.result())) -> true
                    test
                }
              }
              case _ => Left(InternalError("the value for key "+key+" was not a list",LogType.printError,true)) -> true
            }
            case "$neq" => validfunc(field._2) match {
              case Left(e) => Left(e) -> true
              case Right(result) => Right(field._1 -> result) -> true
            }
            case _ => Right(field) -> false
          }
          case None => Left(InternalError("empty object for key "+field._1,LogType.printError,true)) -> true
        }
        case _ => validfunc(field._2) match {
          case Left(e) => Left(e) -> true
          case Right(result) => Right(field._1 -> result) -> true
        }
      }
  }
  private def isBaseArrayValueValid[T <: AnyRef](field:(String,AnyRef))
                                          (validfunc:(Any) => Either[InternalError,Any]):(Either[InternalError,(String,Any)],Boolean) = {
    isBaseValueValid(field)(validfunc) match {                //todo remember bug with $in/$nin and arrays
      case (newAcc,true) => newAcc-> true
      case (Right(_),false) => field._2 match {
        case dbo:BasicDBObject => dbo.headOption match {
          case Some((key,value)) => key match {
            case "$all" => value match {
              case dblist:BasicDBList => {
                val listBuilder = MongoDBList.newBuilder
                dblist.find(v => validfunc(v) match {
                  case Left(e) => true
                  case Right(result) => listBuilder += result; false
                }) match {
                  case Some(value) => Left(InternalError("the value "+value.toString+" was invalid for key"+key,LogType.printError,true)) -> true
                  case None => Right(field._1 -> MongoDBObject(key -> listBuilder.result())) -> true
                }
              }
              case _ => Left(InternalError("the value for key "+key+" was not a list",LogType.printError,true)) -> true
            }
            case "$size" => if (value.isInstanceOf[Int]) Right(field._1 -> MongoDBObject(key -> value)) -> true
                            else Left(InternalError("$size special operator found but without a number value",LogType.printError,true)) -> true
            case _ => Right(field) -> false
          }
          case None => Left(InternalError("empty object for key "+field._1,LogType.printError,true)) -> true
        }
        case _ => validfunc(field._2) match {
          case Left(e) => Left(e) -> true
          case Right(result) => Right(field._1 -> result) -> true
        }
      }
      case _ => throw new RuntimeException("should never get here")
    }
  }
  def isStringValueValid[T <: AnyRef](field:(String,AnyRef))
                                     (validfunc:(Any) => Either[InternalError,Any]):Either[InternalError,(String,Any)] = {
    isBaseValueValid(field)(validfunc) match {
      case (Left(e),true) => Left(e)
      case (Right(newField),true) => Right(newField)
      case (Right(_),false) => Left(InternalError("invalid value for key: "+field._1,LogType.printError,true))
      case _ => throw new RuntimeException("should never get here")
    }
  }
  def isStringArrayValueValid[T <: AnyRef](field:(String,AnyRef))
                                          (validfunc:(Any) => Either[InternalError,Any]):Either[InternalError,(String,Any)] = {
    isBaseArrayValueValid(field)(validfunc) match {
      case (Left(e),true) => Left(e)
      case (Right(newField),true) => Right(newField)
      case (Right(_),false) => Left(InternalError("invalid value for key: "+field._1,LogType.printError, true))
      case _ => throw new RuntimeException("should never get here")
    }
  }
  def isNumberValueValid[T <: AnyRef](field:(String,AnyRef))
                                     (validfunc:(Any) => Either[InternalError,Any]):Either[InternalError,(String,Any)] = {
    isBaseValueValid(field)(validfunc) match {
      case (Left(e),true) => Left(e)
      case (Right(newField),true) => Right(newField)
      case (Right(builder),false) => field._2 match {
        case dbo:BasicDBObject => {
          val newBuilder = MongoDBObject.newBuilder
          dbo.iterator.find(innerField => innerField._1 match {
            case "$gt" | "$lt" | "$gte" | "$lte" => validfunc(innerField._2) match {
              case Left(e) => true
              case Right(result) => newBuilder += (innerField._1 -> result); false
            }
            case _ => true
          }) match {
            case Some((key,_)) => Left(InternalError("invalid value for key: "+key,LogType.printError,true))
            case None => Right(field._1 -> newBuilder.result())
          }
        }
        case _ => Left(InternalError("invalid value for key: "+field._1,LogType.printError,true))
      }
      case _ => throw new RuntimeException("should never get here")
    }
  }
  def isObjectValueValid[T <: AnyRef](field:(String,AnyRef))
                                     (validfunc:(Any) => Either[InternalError,Any]):Either[InternalError,(String,Any)] = {
    isBaseValueValid(field)(validfunc) match {
      case (Left(e),true) => Left(e)
      case (Right(newField),true) => Right(newField)
      case (Right(builder),false) => Left(InternalError("invalid value for key: "+field._1,LogType.printError,true))
      case _ => throw new RuntimeException("should never get here")
    }
  }
  def isObjectArrayValueValid[T <: AnyRef](field:(String,AnyRef))
                                          (validfunc:(Any) => Either[InternalError,Any]):Either[InternalError,(String,Any)] = {
    isBaseArrayValueValid(field)(validfunc) match {
      case (Left(e),true) => Left(e)
      case (Right(newField),true) => Right(newField)
      case (Right(builder),false) => Left(InternalError("invalid value for key: "+field._1,LogType.printError,true))
      case _ => throw new RuntimeException("should never get here")
    }
  }
}

