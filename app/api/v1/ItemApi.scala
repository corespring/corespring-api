package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.Logger
import api.{InvalidFieldException, ApiError, QueryHelper}
import models.{Organization, ContentCollection, Content, Item}
import com.mongodb.util.JSONParseException
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.SalatInsertError
import play.api.templates.Xml
import play.api.mvc.Result
import play.api.libs.json.{JsValue, Json}
import com.typesafe.config.ConfigFactory
import models.mongoContext._
import controllers.JsonValidationException
import api.processors.FeedbackProcessor._

/**
 * Items API
 */

object ItemApi extends BaseApi {

  //todo: confirm with evaneus this is what should be returned by default for /items/:id
  val excludedFieldsByDefault = Some(MongoDBObject(
    Item.copyrightOwner -> 0,
    Item.credentials -> 0,
    //Item.supportingMaterials -> 0,
    //Item.keySkills -> 0,
    Item.contentType -> 0
    //Item.data -> 0
  ))

  val dataField = MongoDBObject(Item.data -> 1, Item.collectionId -> 1)
  val excludeData = Some(MongoDBObject(Item.data -> 0))

  /**
   * List query implementation for Items
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    val fields = if ( f.isDefined ) f else excludedFieldsByDefault
    doList(request.ctx.organization, q, fields, c, sk, l)
  }

  def listWithOrg(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    if (Organization.isChild(request.ctx.organization,orgId)) {
      doList(orgId, q, f, c, sk, l)
    } else Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    if (ContentCollection.isAuthorized(request.ctx.organization,collId,Permission.All)) {
      val initSearch = MongoDBObject(Content.collectionId -> collId.toString)
      QueryHelper.list(q, f, c, sk, l, Item, Some(initSearch))
    } else Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  private def doList(orgId: ObjectId, q: Option[String], f: Option[Object], c: String, sk: Int, l: Int) = {
    val initSearch = MongoDBObject(Content.collectionId -> MongoDBObject("$in" -> ContentCollection.getCollectionIds(orgId,Permission.All).map(_.toString)))
    QueryHelper.list(q, f, c, sk, l, Item, Some(initSearch))
  }

  /**
   * Returns an Item.  Only the default fields are rendered back.
   */
  def getItem(id: ObjectId) = ApiAction { request =>
    _getItem(request.ctx.organization, id, excludedFieldsByDefault)
  }

  /**
   * Returns an Item with all its fields.
   *
   * @param id
   * @return
   */
  def getItemDetail(id: ObjectId) = ApiAction { request =>
    _getItem(request.ctx.organization, id, excludeData)
  }

  /**
   * Helper method to retrieve Items from mongo.
   *
   * @param id
   * @param fields
   * @return
   */
  private def _getItem(callerOrg: ObjectId, id: ObjectId, fields: Option[DBObject]): Result  = {
    fields.map(Item.collection.findOneByID(id, _)).getOrElse( Item.collection.findOneByID(id)) match {
      case Some(o) =>  o.get(Item.collectionId) match {
        case collId:String => if (Content.isCollectionAuthorized(callerOrg, collId, Permission.All)) {
          val i = grater[Item].asObject(o)
          Ok(Json.toJson(i))
        } else {
          Forbidden
        }
        case _ => Forbidden
      }
      case _ => NotFound
    }
  }

  /**
   * Returns the raw content body for the item
   *
   * @param id
   * @return
   */
  def getItemData(id: ObjectId) = ApiAction { request =>
    Item.collection.findOneByID(id, dataField) match {
      case Some(o) => o.get(Item.collectionId) match {
        case collId:String => if ( Content.isCollectionAuthorized(request.ctx.organization, collId,Permission.All)) {
          // added this to prevent a NPE when the data is not available in the item
          // this is temporary until bleezmo finishes working on this operation
          if (o.contains(Item.data))
            Ok(Xml(o.get(Item.data).toString))
          else
            Ok("")
        } else {
          Forbidden
        }
        case _ => Forbidden
      }
      case _ => NotFound
    }
  }

  /**
   * Deletes the item matching the id specified
   *
   * @param id
   * @return
   */
  def deleteItem(id: ObjectId) = ApiAction { request =>
    Item.collection.findOneByID(id, MongoDBObject(Item.collectionId -> 1)) match {
      case Some(o) => o.get(Item.collectionId) match {
        case collId:String => if ( Content.isCollectionAuthorized(request.ctx.organization, collId, Permission.All) ) {
          Content.moveToArchive(id) match {
            case Right(_) => Ok(com.mongodb.util.JSON.serialize(o))
            case Left(error) => InternalServerError(Json.toJson(ApiError.DeleteItem(error.clientOutput)))
          }
        } else {
          Forbidden
        }
        case _ => Forbidden
      }
      case _ => NotFound
    }
  }

  def createItem = ApiAction { request =>
    request.body.asJson match {
      case Some(jsonData) => {
        val json = addFeedbackIds(jsonData.toString)
        try {
          val dbObj = com.mongodb.util.JSON.parse(json).asInstanceOf[DBObject]
          if (dbObj.isDefinedAt("id")) {
            BadRequest(Json.toJson(ApiError.IdNotNeeded))
          } else {
            val i: Item = Json.parse(json).as[Item]
            if (i.collectionId.isEmpty) {
              BadRequest(Json.toJson(ApiError.CollectionIsRequired))
            } else if (Content.isCollectionAuthorized(request.ctx.organization, i.collectionId, Permission.All)) {
              Item.insert(i) match {
                case Some(_) => Ok(removeFeedbackIds(addOutcomeIdentifiers(Json.toJson(i).toString)))
                case None => InternalServerError(Json.toJson(ApiError.CantSave))
              }
            } else {
              Forbidden(Json.toJson(ApiError.CollectionUnauthorized))
            }
          }
        } catch {
          case parseEx: JSONParseException => BadRequest(Json.toJson(ApiError.JsonExpected))
          case invalidField: InvalidFieldException => BadRequest(Json.toJson(ApiError.InvalidField.format(invalidField.field)))
          case e:SalatInsertError => InternalServerError(Json.toJson(ApiError.CantSave))
        }
      }
      case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
    }
  }

  def updateItem(id: ObjectId) = ApiAction { request =>
    if (Content.isAuthorized(request.ctx.organization, id, Permission.All)) {
      request.body.asJson match {
        case Some(jsonData) => {
          val json = Json.parse(addFeedbackIds(jsonData.toString))
          if ((json \ Item.id).asOpt[String].isDefined) {
            BadRequest(Json.toJson(ApiError.IdNotNeeded))
          }
          else if ((json \ Item.collectionId).asOpt[String].isDefined) {
            BadRequest(Json.toJson(ApiError.CollIdNotNeeded))
          }
          else {
            try {
              Item.updateItem(id,Json.fromJson[Item](json),excludedFieldsByDefault) match {
                case Right(i) => Ok(removeFeedbackIds(Json.toJson(i).toString))
                case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateItem(error.clientOutput)))
              }
            } catch {
              case e:JSONParseException => BadRequest(Json.toJson(ApiError.JsonExpected))
              case e:JsonValidationException => BadRequest(Json.toJson(ApiError.JsonExpected(Some(e.getMessage))))
            }
          }
        }
        case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
      }
    } else Forbidden
  }

  def getItemsInCollection(collId: ObjectId) = ApiAction { request =>
    NotImplemented
  }
}
