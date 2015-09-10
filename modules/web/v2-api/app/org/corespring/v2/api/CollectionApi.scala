package org.corespring.v2.api

import com.mongodb.casbah.commons.TypeImports._
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError
import org.corespring.services.{ ContentCollectionService, ContentCollectionUpdate }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.web.api.v1.errors.ApiError
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class CollectionApi(
  contentCollectionService: ContentCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  jsonFormatting: JsonFormatting,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {
  override implicit def ec: ExecutionContext = v2ApiContext.context

  import jsonFormatting.writeContentCollection

  private def canRead(collectionId: ObjectId)(fn: OrgAndOpts => SimpleResult): Action[AnyContent] = Action.async { r =>

    val identity = for {
      identity <- getOrgAndOptionsFn(r)
      _ <- if (identity.org.contentcolls.exists(_.collectionId == collectionId)) Success(true) else Failure(generalError("org can't access this collection id"))
    } yield identity

    Future(identity match {
      case Success(identity) => fn(identity)
      case Failure(e) => Status(e.statusCode)(e.message)
    })
  }

  private def someToFailure[A](o: Option[A], e: => V2Error) = o match {
    case Some(_) => e.asFailure
    case _ => Success(true)
  }

  def createCollection = futureWithIdentity { (identity, request) =>
    Future {

      val v: Validation[V2Error, ContentCollection] = for {
        json <- request.body.asJson.toSuccess(noJson)
        _ <- someToFailure((json \ "id").asOpt[String], propertyNotAllowedInJson("id", json))
        name <- (json \ "name").asOpt[String].toSuccess(propertyNotFoundInJson("name"))
        _ <- if (name.isEmpty) generalError("Name is empty").asFailure else Success(true)
        createResult <- contentCollectionService.create(name, identity.org).v2Error
      } yield createResult
      v.map(c => Json.toJson(c)).toSimpleResult(CREATED)
    }
  }

  def deleteCollection(id: ObjectId) = futureWithIdentity {
    (identity, request) =>
      Future {
        val v: Validation[V2Error, Boolean] = for {
          canAccess <- orgCanAccess(identity.org, id, Permission.Write)
          _ <- contentCollectionService.delete(id).v2Error
        } yield true

        v.map(ok => Json.obj()).toSimpleResult()
      }
  }

  type ShareFunction[Q] = (ObjectId, Q, ObjectId) => Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  private def handleShare[Q](collectionId: ObjectId, shareFn: ShareFunction[Q], buildQuery: Request[AnyContent] => Validation[V2Error, Q]) = {
    futureWithIdentity { (identity, request) =>
      Future {
        val v: Validation[V2Error, Seq[VersionedId[ObjectId]]] = for {
          //json <- request.body.asJson.toSuccess(noJson)
          //itemIds <- (json \ "items").asOpt[Seq[String]].filterNot(_.length == 0).toSuccess(propertyNotFoundInJson("items"))
          //versionedIds <- Success(itemIds.flatMap(VersionedId(_)))
          q <- buildQuery(request)
          itemsAdded <- shareFn(identity.org.id, q, collectionId).v2Error
        } yield itemsAdded

        v.map(ids => ids.map(_.id.toString)).map(Json.toJson(_)).toSimpleResult()
      }

    }
  }

  private def idsFromRequest(r: Request[AnyContent]): Validation[V2Error, Seq[VersionedId[ObjectId]]] = for {
    json <- r.body.asJson.toSuccess(noJson)
    itemIds <- (json \ "items").asOpt[Seq[String]].filterNot(_.length == 0).toSuccess(propertyNotFoundInJson("items"))
    versionedIds <- Success(itemIds.flatMap(VersionedId(_)))
  } yield versionedIds

  def shareItemsWithCollection(collectionId: ObjectId) = handleShare(
    collectionId,
    contentCollectionService.shareItems,
    idsFromRequest)

  def unShareItemsWithCollection(collectionId: ObjectId) = handleShare(
    collectionId,
    contentCollectionService.unShareItems,
    idsFromRequest)

  /**
   * Add the items retrieved by the given query (see ItemApi.list for similar query) to the specified collection
   * @param q - the query to select items to add to the collection
   * @param id  - collection to add the items to
   * @return  - json with success or error response
   */
  def shareFilteredItemsWithCollection(collectionId: ObjectId, q: Option[String]) = {
    handleShare(collectionId, contentCollectionService.shareItemsMatchingQuery, (r) => {
      q match {
        case Some(str) => Success(str)
        case _ => propertyNotFoundInJson("q").asFailure
      }
    })
  }

  /*
   * Add the items retrieved by the given query (see ItemApi.list for similar query) to the specified collection
   * @param q - the query to select items to add to the collection
   * @param id  - collection to add the items to
   * @return  - json with success or error response

  def shareFilteredItemsWithCollection(id: ObjectId, q: Option[String]) = ApiActionWrite { request =>
    contentCollectionService.findOneById(id) match {
      case Some(coll) => if (contentCollectionService.isAuthorized(request.ctx.organization, id, Permission.Write)) {
        if (q.isDefined) {
          contentCollectionService.shareItemsMatchingQuery(request.ctx.organization, q.get, id) match {
            case Right(itemsAdded) => Ok(toJson(itemsAdded.size))
            case Left(error) => InternalServerError(Json.toJson(ApiError.ItemSharingError(Some(error.message))))
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
  */
  /*
 def unShareItemsWithCollection(collectionId: ObjectId) = ApiActionWrite {
    request =>
      request.body.asJson match {
        case Some(json) => {
          if ((json \ "items").asOpt[Array[String]].isEmpty) {
            BadRequest(Json.toJson(ApiError.ItemSharingError(Some("no items could be found in request body json"))))
          } else {
            val itemIds = (json \ "items").as[Seq[String]]
            val versionedItemIds = itemIds.map(VersionedId(_)).flatten
            contentCollectionService.unShareItems(request.ctx.organization, versionedItemIds, Seq(collectionId)) match {
              case Right(itemsAdded) => Ok(toJson(itemsAdded.map(versionedId => versionedId.id.toString)))
              case Left(error) => InternalServerError(Json.toJson(ApiError.ItemSharingError(Some(error.message))))
            }
          }
        }
        case _ => jsonExpected
      }
  }

   */
  /*
  def shareItemsWithCollection(collectionId: ObjectId) = ApiActionWrite {
    request =>
      request.body.asJson match {
        case Some(json) => {
          if ((json \ "items").asOpt[Array[String]].isEmpty) {
            BadRequest(Json.toJson(ApiError.ItemSharingError(Some("no items could be found in request body json"))))
          } else {
            val itemIds = (json \ "items").as[Seq[String]]
            val versionedItemIds = itemIds.map(VersionedId(_)).flatten
            contentCollectionService.shareItems(request.ctx.organization, versionedItemIds, collectionId) match {
              case Right(itemsAdded) => Ok(toJson(itemsAdded.map(versionedId => versionedId.id.toString)))
              case Left(error) => InternalServerError(Json.toJson(ApiError.ItemSharingError(Some(error.message))))
            }
          }
        }
        case _ => jsonExpected
      }
  }*/
  /*def deleteCollection(id: ObjectId) = ApiActionWrite { request =>
  contentCollectionService.findOneById(id) match {
    case Some(coll) => if (contentCollectionService.itemCount(id) == 0 && contentCollectionService.isAuthorized(request.ctx.organization, id, Permission.Write)) {
      contentCollectionService.delete(id) match {
        case Success(_) => Ok(Json.toJson(coll))
        case Failure(error) => InternalServerError(Json.toJson(ApiError.DeleteCollection(Some(error.message))))
      }
    } else {
      InternalServerError(Json.toJson(ApiError.DeleteCollection(Some("cannot delete collection that contains items"))))
    }
    case None => BadRequest(Json.toJson(ApiError.DeleteCollection))
  }
}*/

  private def orgCanAccess(org: Organization, collectionId: ObjectId, p: Permission): Validation[V2Error, Boolean] = {

    val o = for {
      ref <- org.contentcolls.find(_.collectionId == collectionId)
      refPerm <- Permission.fromLong(ref.pval)
      hasPermission <- Some(p.has(refPerm))
    } yield true

    o match {
      case Some(true) => Success(true)
      case _ => Failure(generalError(s"Org ${org.id}, can't access $collectionId"))
    }
  }

  def updateCollection(collectionId: ObjectId) = futureWithIdentity { (identity, request) =>

    lazy val organizationsNotSupported = noLongerSupported("'organizations' is no longer supported in the json request body to update collection")

    Future {
      val v: Validation[V2Error, JsValue] = for {
        _ <- orgCanAccess(identity.org, collectionId, Permission.Write)
        json <- request.body.asJson.toSuccess(noJson)
        _ <- someToFailure((json \ "organizations").asOpt[JsObject], organizationsNotSupported)
        update <- Success(ContentCollectionUpdate((json \ "name").asOpt[String], (json \ "isPublic").asOpt[Boolean]))
        result <- contentCollectionService.update(collectionId, update).v2Error
      } yield Json.toJson(result)

      v.toSimpleResult()
    }
  }

  def getCollection(collectionId: ObjectId) = canRead(collectionId) { orgAndOpts =>
    contentCollectionService
      .findOneById(collectionId)
      .map(c => Ok(Json.toJson(c)))
      .getOrElse(NotFound)
  }

  def listWithOrg(
    q: Option[String] = None,
    f: Option[String] = None,
    c: Boolean = false,
    sk: Int = 0,
    l: Int = 50,
    sort: Option[String] = None) = list(q, f, c, sk, l, sort)

  /** Note: ignoring q,f,c,sk,sort and l for this 1st iteration */
  def list(
    q: Option[String] = None,
    f: Option[String] = None,
    c: Boolean = false,
    sk: Int = 0,
    l: Int = 50,
    sort: Option[String] = None) = Action.async { request =>

    Future {
      getOrgAndOptionsFn(request).map { identity =>
        val collectionList = contentCollectionService.listCollectionsByOrg(identity.org.id)
        Ok(Json.toJson(collectionList.toSeq))
      }.getOrElse(Unauthorized)
    }
  }
}
