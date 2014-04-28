package api.v1

import api.ApiError
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.TypeImports.ObjectId
import com.mongodb.casbah.map_reduce._
import com.novus.salat.dao.SalatMongoCursor
import controllers.auth.BaseApi
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.models.search.SearchCancelled
import org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._
import org.corespring.platform.core.models.{ Organization, CollectionExtraDetails, ContentCollection }
import org.corespring.platform.core.services.item.ItemServiceImpl
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc.Result
import scala.Some
import scalaz.Failure
import scalaz.Success
import com.mongodb.casbah.map_reduce.MapReduceCommand
import com.mongodb.casbah.map_reduce.MapReduceInlineOutput

/**
 * The Collections API
 */

object CollectionApi extends BaseApi {
  /**
   * Returns a list of collections visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiActionRead {
    request =>
      doList(request.ctx.organization, q, f, c, sk, l, sort)
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiActionRead {
    request =>
      if (Organization.getTree(request.ctx.organization).exists(_.id == orgId)) {
        doList(orgId, q, f, c, sk, l, sort)
      } else
        Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  val fieldValueMap = Map(
    "itemType" -> "taskInfo.itemType",
    "contributor" -> "contributorDetails.contributor"
  )

  def fieldValuesByFrequency(collectionIds: String, fieldName: String) = ApiActionRead { request =>
    fieldValueMap.get(fieldName) match {
      case Some(field) => {
        // Expand "one.two" into Seq("this.one", "this.one.two") for checks down path in a JSON object
        val fieldCheck =
          field.split("\\.").foldLeft(Seq.empty[String])((acc, str) =>
            acc :+ (if (acc.isEmpty) s"this.$str" else s"${acc.last}.$str")).mkString(" && ")
        val cmd = MapReduceCommand(
          input = "content",
          map = s"""
            function() {
              if (${fieldCheck}) {
                emit(this.$field, 1);
              }
            }""",
          reduce = s"""
            function(previous, current) {
              var count = 0;
              for (index in current) {
                count += current[index];
              }
              return count;
            }""",
          query = Some(MongoDBObject("collectionId" -> MongoDBObject("$in" -> collectionIds.split(",").toSeq))),
          output = MapReduceInlineOutput
        )

        ItemServiceImpl.collection.mapReduce(cmd) match {
          case result: MapReduceInlineResult => {
            val fieldValueMap = result.map(_ match {
              case dbo: DBObject => {
                Some(dbo.get("_id").toString -> dbo.get("value").asInstanceOf[Double])
              }
              case _ => None
            }).flatten.toMap
            Ok(Json.prettyPrint(Json.toJson(fieldValueMap)))
          }
          case _ => BadRequest(Json.toJson(ApiError.InvalidField))
        }
      }
      case _ => BadRequest(Json.toJson(ApiError.InvalidField))
    }
  }

  private def doList(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, optsort: Option[String]) = {
    val collrefs = ContentCollection.getContentCollRefs(orgId, Permission.Read, true)
    val initSearch = MongoDBObject("_id" -> MongoDBObject("$in" -> collrefs.map(_.collectionId)))
    def applySort(colls: SalatMongoCursor[ContentCollection]): Result = {
      optsort.map(ContentCollection.toSortObj(_)) match {
        case Some(Right(sort)) => Ok(Json.toJson(colls.sort(sort).skip(sk).limit(l).toSeq.map(c => CollectionExtraDetails(c, collrefs.find(_.collectionId == c.id).get.pval))))
        case None => Ok(Json.toJson(colls.skip(sk).limit(l).toSeq.map(c => CollectionExtraDetails(c, collrefs.find(_.collectionId == c.id).get.pval))))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidSort(error.clientOutput)))
      }
    }
    q.map(ContentCollection.toSearchObj(_, Some(initSearch))).getOrElse[Either[SearchCancelled, MongoDBObject]](Right(initSearch)) match {
      case Right(query) => f.map(ContentCollection.toFieldsObj(_)) match {
        case Some(Right(searchFields)) => if (c == "true") Ok(JsObject(Seq("count" -> JsNumber(ContentCollection.find(query).count))))
        else applySort(ContentCollection.find(query, searchFields.dbfields))
        case None => if (c == "true") Ok(JsObject(Seq("count" -> JsNumber(ContentCollection.find(query).count))))
        else applySort(ContentCollection.find(query))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidFields(error.clientOutput)))
      }
      case Left(sc) => sc.error match {
        case None => Ok(JsArray(Seq()))
        case Some(error) => BadRequest(Json.toJson(ApiError.InvalidQuery(error.clientOutput)))
      }
    }
  }

  /**
   * Returns a Collection by its id
   *
   * @param id The collection id
   * @return
   */
  def getCollection(id: ObjectId) = ApiActionRead {
    request =>
      ContentCollection.findOneById(id) match {
        case Some(org) => {
          // todo: check if this collection is visible to the caller?
          Ok(Json.toJson(org))
        }
        case _ => NotFound
      }
  }

  /**
   * Creates a Collection
   *
   * @return
   */
  def createCollection = ApiActionWrite { request =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            val newId = new ObjectId
            val name = (json \ "name").asOpt[String]
            if (name.isEmpty) {
              BadRequest(Json.toJson(ApiError.CollectionNameMissing))
            } else {
              val collection = ContentCollection(id = newId, name = name.get, ownerOrgId = request.ctx.organization)
              ContentCollection.insertCollection(request.ctx.organization, collection, Permission.Write) match {
                case Right(coll) => Ok(Json.toJson(CollectionExtraDetails(coll, Permission.Write.value)))
                case Left(e) => InternalServerError(Json.toJson(ApiError.InsertCollection(e.clientOutput)))
              }
            }
          }
        }
      }
      case _ => jsonExpected
    }
  }

  private def unknownCollection = NotFound(Json.toJson(ApiError.UnknownCollection))

  private def addCollectionToOrganizations(values: Seq[JsValue], collId: ObjectId): Either[InternalError, Unit] = {
    val orgs: Seq[(ObjectId, Permission)] = values.map(v => v match {
      case JsString(strval) => (new ObjectId(strval) -> Permission.Read)
      case JsObject(orgWithPerm) => (new ObjectId(orgWithPerm(1)._1) -> Permission.fromLong(orgWithPerm(1)._2.as[Long]).get)
      case _ => return Right(InternalError("incorrect format for organizations"))
    })
    ContentCollection.addOrganizations(orgs, collId)
  }

  /**
   * Updates a collection
   *
   * @return
   */
  def updateCollection(id: ObjectId) = ApiActionWrite {
    request =>
      ContentCollection.findOneById(id).map(original => {
        request.body.asJson match {
          case Some(json) => {
            val name = (json \ "name").asOpt[String].getOrElse(original.name)
            val toUpdate = ContentCollection(name, id = original.id, ownerOrgId = original.ownerOrgId)
            if ((Organization.getPermissions(request.ctx.organization, original.id).value & Permission.Read.value) == Permission.Read.value) {
              ContentCollection.updateCollection(toUpdate) match {
                case Right(coll) => (json \ "organizations") match {
                  case JsArray(values) => addCollectionToOrganizations(values, id) match {
                    case Right(_) => Ok(Json.toJson(coll))
                    case Left(e) => InternalServerError(Json.toJson(ApiError.AddToOrganization(e.clientOutput)))
                  }
                  case JsUndefined() => Ok(Json.toJson(coll))
                  case _ => BadRequest(Json.toJson(ApiError.UpdateCollection(Some("organizations was included but was not the right format"))))
                }
                case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateCollection(e.clientOutput)))
              }
            } else Unauthorized(Json.toJson(ApiError.UpdateCollection(Some("you do not have permission to update this collection"))))
          }
          case _ => jsonExpected
        }
      }).getOrElse(unknownCollection)
  }

  def setEnabledStatus(id: ObjectId, enabled: Boolean) = ApiActionWrite { request =>
    Organization.setCollectionEnabledStatus(request.ctx.organization, id, enabled) match {
      case Left(error) => InternalServerError(Json.toJson(ApiError.DeleteCollection(error.clientOutput)))
      case Right(collRef) => {
        Ok(Json.toJson(s"updated ${collRef.collectionId.toString}"))
      }
    }
  }

  /**
   * Shares a collection with an organization, will fail if the context organization is not the same as
   * the owner organization for the collection
   * @param collectionId
   * @param destinationOrgId
   * @return
   */
  def shareCollection(collectionId: ObjectId, destinationOrgId: ObjectId) = ApiActionWrite {  request =>
    ContentCollection.findOneById(collectionId) match {
      case Some(collection) =>
        if (collection.ownerOrgId == request.ctx.organization) {
          Organization.addCollection(destinationOrgId,collectionId, Permission.Read) match {
            case Left(error) => InternalServerError(Json.toJson(ApiError.AddToOrganization(error.clientOutput)))
            case Right(collRef) =>  Ok(Json.toJson("updated" + collRef.collectionId.toString))
          }
        } else {
          InternalServerError(Json.toJson(ApiError.AddToOrganization(Some("context org does not own collection"))))
        }

      case None =>  InternalServerError(Json.toJson(ApiError.AddToOrganization(Some("collection not found"))))
    }
  }

  /**
   * Deletes a collection
   */
  def deleteCollection(id: ObjectId) = ApiActionWrite { request =>
    ContentCollection.findOneById(id) match {
      case Some(coll) => if (coll.itemCount == 0 && ContentCollection.isAuthorized(request.ctx.organization, id, Permission.Write)) {
        ContentCollection.delete(id) match {
          case Success(_) => Ok(Json.toJson(coll))
          case Failure(error) => InternalServerError(Json.toJson(ApiError.DeleteCollection(error.clientOutput)))
        }
      } else {
        InternalServerError(Json.toJson(ApiError.DeleteCollection(Some("cannot delete collection that contains items"))))
      }
      case None => BadRequest(Json.toJson(ApiError.DeleteCollection))
    }
  }

  /**
   * Add items to the collection specified.
   * receives list of items in json in request body
   * @param collectionId
   * @return
   */
  def shareItemsWithCollection(collectionId: ObjectId) = ApiActionWrite {
    request =>
      request.body.asJson match {
        case Some(json) => {
          if ((json \ "items").asOpt[Array[String]].isEmpty) {
            BadRequest(Json.toJson(ApiError.ItemSharingError(Some("no items could be found in request body json"))))
          } else {
            val itemIds = (json \ "items").as[Seq[String]]
            val versionedItemIds = itemIds.map(stringToVersionedId).flatten
            ContentCollection.shareItems(request.ctx.organization, versionedItemIds, collectionId) match {
              case Right(itemsAdded) => Ok(toJson(itemsAdded.map(versionedId => versionedId.id.toString)))
              case Left(error) => InternalServerError(Json.toJson(ApiError.ItemSharingError(error.clientOutput)))
            }
          }
        }
        case _ => jsonExpected
      }
  }

  /**
   * Unshare items from the collection specified.
   * receives list of items in json in request body
   * @param collectionId
   * @return
   */
  def unShareItemsWithCollection(collectionId: ObjectId) = ApiActionWrite {
    request =>
      request.body.asJson match {
        case Some(json) => {
          if ((json \ "items").asOpt[Array[String]].isEmpty) {
            BadRequest(Json.toJson(ApiError.ItemSharingError(Some("no items could be found in request body json"))))
          } else {
            val itemIds = (json \ "items").as[Seq[String]]
            val versionedItemIds = itemIds.map(stringToVersionedId).flatten
            ContentCollection.unShareItems(request.ctx.organization, versionedItemIds, Seq(collectionId)) match {
              case Right(itemsAdded) => Ok(toJson(itemsAdded.map(versionedId => versionedId.id.toString)))
              case Left(error) => InternalServerError(Json.toJson(ApiError.ItemSharingError(error.clientOutput)))
            }
          }
        }
        case _ => jsonExpected
      }
  }

  /**
   * Add the items retrieved by the given query (see ItemApi.list for similar query) to the specified collection
   * @param q - the query to select items to add to the collection
   * @param id  - collection to add the items to
   * @return  - json with success or error response
   */
  def shareFilteredItemsWithCollection(id: ObjectId,q: Option[String]) = ApiActionWrite {  request =>
    ContentCollection.findOneById(id) match {
      case Some(coll) => if (ContentCollection.isAuthorized(request.ctx.organization, id, Permission.Write)) {
        if (q.isDefined) {
          ContentCollection.shareItemsMatchingQuery(request.ctx.organization,q.get,id) match {
            case Right(itemsAdded) => Ok(toJson(itemsAdded.size))
            case Left(error) => InternalServerError(Json.toJson(ApiError.ItemSharingError(error.clientOutput)))
          }
        } else {
          BadRequest(Json.toJson(ApiError.ItemSharingError(Some("q is required parameter"))))
        }

      } else {
        Forbidden(Json.toJson(ApiError.ItemSharingError(Some("permission not granted"))))
      }
      case None => BadRequest(Json.toJson(ApiError.ItemSharingError(Some("collection not found"))))
    }
  }

}
