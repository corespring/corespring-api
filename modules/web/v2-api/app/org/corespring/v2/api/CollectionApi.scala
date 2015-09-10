package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.services.{ ContentCollectionService, ContentCollectionUpdate }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.{ Action, AnyContent, RequestHeader, SimpleResult }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class CollectionApi(
  contentCollectionService: ContentCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {
  override implicit def ec: ExecutionContext = v2ApiContext.context

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

  def createCollection = futureWithIdentity { (identity, request) =>

    val result: Validation[V2Error, ContentCollection] = for {
      json <- request.body.asJson.toSuccess(noJson)
      _ <- (json \ "id").asOpt[String].map(_ => propertyNotAllowedInJson("id", json)).getOrElse(Success(true))
      name <- (json \ "name").asOpt[String].toSuccess(propertyNotFoundInJson("name"))
      _ <- if (name.isEmpty) Failure(generalError("Name is empty")) else Success(true)
      createResult <- contentCollectionService.create(name, identity.org)
    } yield createResult

    Future(validationToResult[ContentCollection](c => Created(Json.toJson(c)))(result))
  }

  private def orgCanAccess(org: Organization, collectionId: ObjectId): Validation[V2Error, Boolean] = {
    if (org.contentcolls.exists(r => r.collectionId == collectionId)) {
      Success(true)
    } else {
      Failure(generalError(s"Org ${org.id}, can't access $collectionId"))
    }
  }

  def updateCollection(collectionId: ObjectId) = futureWithIdentity { (identity, request) =>

    lazy val organizationsNotSupported = noLongerSupported("'organizations' is no longer supported in the json request body to update collection")

    Future {
      val result: Validation[V2Error, JsValue] = for {
        _ <- orgCanAccess(identity.org, collectionId)
        json <- request.body.asJson.toSuccess(noJson)
        _ <- (json \ "organizations").asOpt[JsObject].map(_ => Failure(organizationsNotSupported).getOrElse(Success(true)))
        update <- Success(ContentCollectionUpdate((json \ "name").asOpt[String], (json \ "isPublic").asOpt[Boolean]))
        result <- contentCollectionService.update(collectionId, update).v2Error
      } yield Json.toJson(result)

      result.toSimpleResult()
    }
  }

  def getCollection(collectionId: ObjectId) = canRead(collectionId) { orgAndOpts =>
    contentCollectionService
      .findOneById(collectionId)
      .map(c => Ok(Json.toJson))
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
        val list = contentCollectionService.listCollectionsByOrg(identity.org.id)
        Ok(Json.toJson(list))
      }.getOrElse(Unauthorized)
    }
  }
}
