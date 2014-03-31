package org.corespring.api.v1

import play.api.Play.current
import com.mongodb.casbah.Imports._
import play.api.cache.Cache

import com.mongodb.casbah.map_reduce.{MapReduceInlineOutput, MapReduceCommand}
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.platform.core.models._
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.json.Json._
import org.corespring.api.v1.fieldValues.{Options, QueryOptions}
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.core.models.search.SearchCancelled
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject
import org.bson.types.ObjectId
import com.mongodb.casbah.map_reduce.MapReduceError
import play.api.Logger
import play.api.libs.json.Json
import org.corespring.api.v1.errors.ApiError
import org.corespring.platform.core.models.auth.Permission

object FieldValuesApi extends BaseApi {

  val FieldValueCacheKey = "fieldValue"

  val AllAvailable = buildAllAvailable

  def buildAllAvailable = {
    val list = FieldValue.descriptions.toList :::
      List(
        ("cc-standard", Standard.description + " (list queries available)"),
        ("subject", Subject.description + " (list queries available)"))
    for { d <- list } yield Map("path" -> ("/api/v1/field_values/all" + d._1), "description" -> d._2)
  }

  def getAllAvailable = Action {
    request =>
      Ok(toJson(AllAvailable))
  }

  /**
   * We store subject and standard in their own collection - so they are query-able.
   * The others exist in a single document in the fieldValues collection.
   * @param fieldName
   * @param q
   * @param f
   * @param c
   * @param sk
   * @param l
   * @return
   */
  def getFieldValues(fieldName: String, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = Action {
    request =>
      val jsValue = getFieldValuesAsJsValue(fieldName, q, f, c, sk, l)
      Ok(toJson(jsValue))
  }

  /**
   * @param fieldOptions -  a map of options for each field, will be extracted by [[org.corespring.api.v1.fieldValues.QueryOptions]]
   * eg:
   * {{{
   *  { "subject" : { q: {}, f: {}, l: 1, sk: 1} , "standards" : {...}}
   * }}}
   */
  def multiple(fieldNames: String, fieldOptions: Option[String], c: String) = Action {
    val names: Seq[String] = fieldNames.split(",").toSeq

    def _getItems(names: Seq[String]): Map[String, JsValue] = names match {
      case Nil => Map()
      case _ => {
        val n: String = names.head
        val options: Options = getOptionsForField(n, fieldOptions)
        val value: JsValue = getFieldValuesAsJsValue(n, options.query, options.filter, c, options.skip, options.limit)
        Map((n -> value)) ++ _getItems(names.tail)
      }
    }
    val items = _getItems(names)
    Ok(toJson(items))
  }

  /**
   * Extract the values from the json string
   */
  private def getOptionsForField(name: String, options: Option[String]): Options = options match {
    case Some(s) => {
      try {
        val json = Json.parse(s)
        (json \ name) match {
          case QueryOptions(queryOpts) => queryOpts
          case _ => QueryOptions.DefaultOptions
        }
      } catch {
        case _: Throwable => QueryOptions.DefaultOptions
      }
    }
    case _ => QueryOptions.DefaultOptions
  }

  private def getFieldValuesAsJsValue(name: String, q: Option[String], f: Option[String], c: String, sk: Int, l: Int): JsValue = {
    name match {
      case "subject" => {
        q.map(Subject.toSearchObj(_, None)).getOrElse[Either[SearchCancelled, DBObject]](Right(MongoDBObject())) match {
          case Right(query) => f.map(Subject.toFieldsObj(_)) match {
            case Some(Right(searchFields)) => if (c == "true") JsObject(Seq("count" -> JsNumber(Subject.find(query).count)))
            else JsArray(Subject.find(query, searchFields.dbfields).toSeq.map(Json.toJson(_)))
            case None => if (c == "true") JsObject(Seq("count" -> JsNumber(Subject.find(query).count)))
            else JsArray(Subject.find(query).toSeq.map(Json.toJson(_)))
            case Some(Left(error)) => JsNull
          }
          case Left(sc) => sc.error match {
            case None => JsArray(Seq())
            case Some(error) => JsNull
          }
        }
      }
      case "cc-standard" => {
        q.map(Standard.toSearchObj(_, None)).getOrElse[Either[SearchCancelled, DBObject]](Right(MongoDBObject())) match {
          case Right(query) => f.map(Standard.toFieldsObj(_)) match {
            case Some(Right(searchFields)) => if (c == "true") JsObject(Seq("count" -> JsNumber(Standard.find(Standard.baseQuery(query)).count)))
            else JsArray(Standard.find(Standard.baseQuery(query), searchFields.dbfields).toSeq.map(Json.toJson(_)))
            case None => if (c == "true") JsObject(Seq("count" -> JsNumber(Standard.find(query).count)))
            else JsArray(Standard.find(Standard.baseQuery(query)).toSeq.map(Json.toJson(_)))
            case Some(Left(error)) => JsNull
          }
          case Left(sc) => sc.error match {
            case None => JsArray(Seq())
            case Some(error) => JsNull
          }
        }
      }
      case _ => {
        Cache.getAs[FieldValue](FieldValueCacheKey) match {
          case None => {
            loadFieldValue()
            Cache.getAs[FieldValue](FieldValueCacheKey) match {
              case None => throw new RuntimeException("Unable to retrieve field value data")
              case Some(fv) => getSubFieldAsJsValue(Some(fv), name)
            }
          }
          case Some(fv) => getSubFieldAsJsValue(Some(fv), name)
        }
      }
    }

  }


  def getFieldValuesByCollection(collectionId: ObjectId) =
    getFieldValuesAction(MongoDBObject("collectionId" -> collectionId.toString))

  def getFieldValuesByContributor(contributor: String) =
    getFieldValuesAction(MongoDBObject("contributorDetails.contributor" -> contributor))

  def getFieldValuesAction() = ApiAction { request =>
    request.ctx.org match {
      case Some(organization) => {
        val collectionIds = ContentCollection.getContentCollRefs(organization.id, Permission.Read, true)
          .map(_.collectionId.toString).distinct
        println(organization.contentcolls.map(_.collectionId.toString))
        getFieldValues(
          MongoDBObject("collectionId" -> MongoDBObject("$in" -> collectionIds))) match {
          case Left(jsValue: JsValue) => Ok(jsValue)
          case Right(error: MapReduceError) => {
            Logger.error(error.errorMessage.getOrElse("Unknown MapReduce error"))
            InternalServerError(Json.toJson(ApiError.MapReduceError))
          }
        }
      }
      case _ => Ok(Json.obj())
    }
  }

  private def getFieldValuesAction(query: MongoDBObject) = Action {
    getFieldValues(query) match {
      case Left(jsValue: JsValue) => Ok(jsValue.toString)
      case Right(error: MapReduceError) => {
        Logger.error(error.errorMessage.getOrElse("Unknown MapReduce error"))
        InternalServerError(Json.toJson(ApiError.MapReduceError))
      }
    }
  }

  /**
   * Returns available field values for gradeLevel, itemType, standard, keySkill, bloomsTaxonomy, and
   * demonstratedKnowledge, scoped by a provided {@link MongoDBObject} for querying.
   */
  private def getFieldValues(query: MongoDBObject): Either[JsValue, MapReduceError] = {

    /**
     * This MapReduce returns results of the form:
     *
     *   {
     *     "_id": {
     *       { "gradeLevel" : "04"}
     *     },
     *     "value": {
     *       { "exists" : 1 }
     *     }
     *   }
     *
     * Where the _id values are the unique key+value pairs for fields and their values.
     */
    val cmd = MapReduceCommand(
      input = "content",
      map = """
            function() {
              if (this.taskInfo) {
                if (this.taskInfo.gradeLevel) {
                  this.taskInfo.gradeLevel.forEach(function(grade) {
                    emit({gradeLevel: grade}, {exists : 1});
                  });
                }
                if (this.taskInfo.itemType) {
                  emit({itemType: this.taskInfo.itemType}, {exists: 1});
                }
                if (this.taskInfo.subjects && this.taskInfo.subjects.primary) {
                  emit({subject: this.taskInfo.subjects.primary}, {exists: 1});
                }
              }

              if (this.standards) {
                this.standards.forEach(function(standard) {
                  emit({standard: standard}, {exists: 1});
                });
              }

              if (this.contributorDetails && this.contributorDetails.contributor) {
                emit({contributor: this.contributorDetails.contributor}, {exists: 1});
              }

              if (this.otherAlignments) {
                if (this.otherAlignments.keySkills) {
                  this.otherAlignments.keySkills.forEach(function(keySkill) {
                    emit({keySkill: keySkill}, {exists: 1});
                  });
                }
                if (this.otherAlignments.bloomsTaxonomy) {
                  emit({bloomsTaxonomy: this.otherAlignments.bloomsTaxonomy}, {exists: 1});
                }
                if (this.otherAlignments.demonstratedKnowledge) {
                  emit({demonstratedKnowledge: this.otherAlignments.demonstratedKnowledge}, {exists: 1});
                }
              }
            }""",
      reduce = """
               function(key, values) {
                 return {exists: 1};
               }""",
      query = Option(query),
      output = MapReduceInlineOutput)

    ItemServiceWired.collection.mapReduce(cmd) match {
      case result: MapReduceInlineResult => {
        val fieldValueMap = result.foldLeft(Map.empty[String, Seq[String]])((acc, obj) => obj match {
          case dbo: DBObject => {
            dbo.get("_id") match {
              // Convert "_id" to key+value pair, and fold into map
              case idObj: BasicDBObject => {
                val key = idObj.keySet.iterator.next
                val value = idObj.getString(key)
                key match {
                  case "subject" => {
                    Subject.findOneById(new ObjectId(value)) match {
                      case Some(subject) => subject.category match {
                        case Some(subjectString) => acc.get(key) match {
                          case Some(seq) => seq.contains(subjectString) match {
                            case true => acc
                            case false => acc + (key -> (seq :+ subjectString))
                          }
                          case None => acc + (key -> Seq(subjectString))
                        }
                        case _ => {
                          Logger.error(s"Could not find subject text for subject with id ${value}")
                          acc
                        }
                      }
                      case _ => {
                        Logger.error(s"Could not find subject with id ${value}")
                        acc
                      }
                    }
                  }
                  case _ => {
                    acc.get(key) match {
                      case Some(seq) => acc + (key -> (seq :+ value))
                      case None => acc + (key -> Seq(value))
                    }
                  }
                }
              }
              case _ => acc
            }

          }
          case _ => acc
        })
        Left(Json.toJson(fieldValueMap))
      }
      case error: MapReduceError => Right(error)
    }
  }

  private def loadFieldValue() {
    FieldValue.findOne(MongoDBObject()) match {
      case Some(fv) => Cache.set(FieldValueCacheKey, fv)
      case _ => //do nothing
    }
  }

  private def getSubFieldAsJsValue(fieldValue: Option[FieldValue], fieldName: String): JsValue = fieldValue match {
    case Some(fv) => {
      FieldValue.getSeqForFieldName(fv, fieldName) match {
        case Some(json) => json
        case _ => JsObject(Seq())
      }
    }
    case _ => JsObject(Seq())
  }
}
