package org.corespring.v2.api

import com.mongodb.casbah.commons.TypeImports._
import org.corespring.models.auth.Permission
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError
import org.corespring.services.{ ContentCollectionService, ContentCollectionUpdate }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
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

  private lazy val logger = Logger(classOf[CollectionApi])

  private def deprecatedMethod(name: String) = {
    logger.warn(s"deprecated method called $name")
  }

  import jsonFormatting.writeContentCollection

  def fieldValuesByFrequency(ids: String, field: String) = Action {
    NotImplemented("Not ready yet!")

    //TODO: move to a service
    /*
    val fieldValueMap = Map(
      "itemType" -> "taskInfo.itemType",
      "contributor" -> "contributorDetails.contributor")

    def fieldValuesByFrequency(collectionIds: String, fieldName: String) = ApiActionRead { request =>

      import com.mongodb.casbah.map_reduce.MapReduceInlineOutput

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
            query = Some(DBObject("collectionId" -> MongoDBObject("$in" -> collectionIds.split(",").toSeq))),
            output = MapReduceInlineOutput)

          ItemServiceWired.collection.mapReduce(cmd) match {
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
    }*/
  }

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

      logger.debug(s"[createCollection]")

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

  /**
   * Shares a collection with an organization, will fail if the context organization is not the same as
   * the owner organization for the collection
   * @param collectionId
   * @param destinationOrgId
   * @return
   */
  def shareCollection(collectionId: ObjectId, destinationOrgId: ObjectId) = futureWithIdentity { (identity, request) =>
    Future {

      logger.debug(s"[shareCollection] collectionId=$collectionId, destinationOrgId=$destinationOrgId")

      val v: Validation[V2Error, ContentCollRef] = for {
        _ <- contentCollectionService.ownsCollection(identity.org, collectionId).v2Error
        o <- contentCollectionService.shareCollectionWithOrg(collectionId, destinationOrgId, Permission.Read).v2Error
      } yield o

      v.map(r => Json.obj("updated" -> r.collectionId.toString)).toSimpleResult()
    }
  }

  def setEnabledStatus(collectionId: ObjectId, enabled: Boolean) = futureWithIdentity { (identity, request) =>
    Future {

      logger.debug(s"[setEnabledStatus], collectionId=$collectionId, enabled=$enabled")

      val toggle: (ObjectId, ObjectId) => Validation[PlatformServiceError, ContentCollRef] = if (enabled) {
        contentCollectionService.enableCollectionForOrg
      } else {
        contentCollectionService.disableCollectionForOrg
      }

      val r = toggle(identity.org.id, collectionId).v2Error
      r.map(c => Json.obj("updated" -> c.collectionId.toString)).toSimpleResult()
    }
  }

  def deleteCollection(collectionId: ObjectId) = futureWithIdentity {
    (identity, request) =>
      Future {
        logger.debug(s"[deleteCollection] collectionId=$collectionId")
        val v: Validation[V2Error, Boolean] = for {
          canAccess <- orgCanAccess(identity.org, collectionId, Permission.Write)
          _ <- contentCollectionService.delete(collectionId).v2Error
        } yield true

        v.map(ok => Json.obj()).toSimpleResult()
      }
  }

  type ShareFunction[Q] = (ObjectId, Q, ObjectId) => Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  private def handleShare[Q](collectionId: ObjectId, shareFn: ShareFunction[Q], buildQuery: Request[AnyContent] => Validation[V2Error, Q]) = {
    futureWithIdentity { (identity, request) =>

      logger.debug(s"[handleShare] collectionId=$collectionId")

      Future {
        val v: Validation[V2Error, Seq[VersionedId[ObjectId]]] = for {
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
   * @param collectionId  - collection to add the items to
   * @return  - json with success or error response
   */
  def shareFilteredItemsWithCollection(collectionId: ObjectId, q: Option[String]) = {
    handleShare[String](collectionId, contentCollectionService.shareItemsMatchingQuery, (r) => {
      q match {
        case Some(str) => Success(str)
        case _ => propertyNotFoundInJson("q").asFailure
      }
    })
  }

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

    logger.info(s"[updateCollection] collectionId=$collectionId")

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

  @deprecated("who uses this?", "core-refactor")
  def listWithOrg(
    orgId: ObjectId,
    q: Option[String] = None,
    f: Option[String] = None,
    c: Option[Boolean] = None,
    sk: Int = 0,
    l: Int = 50,
    sort: Option[String] = None) = {
    deprecatedMethod("listWithOrg")
    list(q, f, c, sk, l, sort)
  }

  /** Note: ignoring q,f,c,sk,sort and l for this 1st iteration */
  def list(
    q: Option[String] = None,
    f: Option[String] = None,
    c: Option[Boolean] = None,
    sk: Int = 0,
    l: Int = 50,
    sort: Option[String] = None) = Action.async { request =>

    logger.info(s"[list] params: q=$q, f=$f, c=$c, sk=$sk, l=$l, sort=$sort")

    Future {
      getOrgAndOptionsFn(request).map { identity =>
        val collectionList = contentCollectionService.listCollectionsByOrg(identity.org.id)
        Ok(Json.toJson(collectionList.toSeq))
      }.getOrElse(Unauthorized)
    }
  }
}