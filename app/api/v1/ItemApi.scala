package api.v1

import controllers.auth.{Permission, BaseApi}
import api.ApiError
import item.QueryCleaner
import models._
import com.mongodb.util.JSONParseException
import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.{SalatMongoCursor, SalatInsertError}
import play.api.templates.Xml
import play.api.mvc.Result
import play.api.libs.json.Json._
import models.mongoContext._
import controllers.{ConcreteS3Service, Log, S3Service, JsonValidationException}
import play.api.libs.json._
import scala.Left
import scala.Some
import scala.Right
import controllers.{Utils, JsonValidationException, InternalError}
import play.api.libs.json.JsObject
import search.{SearchFields, SearchCancelled, ItemSearch}
import models.json.ItemView
import play.api.libs.json.JsObject
import com.typesafe.config.ConfigFactory

/**
 * Items API
 */
class ItemApi(s3service:S3Service) extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  val summaryFields: Seq[String] = Seq(Item.collectionId, Item.gradeLevel, Item.itemType, Item.keySkills, Item.subjects, Item.standards, Item.title)

  private def count(c: String): Boolean = "true".equalsIgnoreCase(c)

  /**
   * List query implementation for Items
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      val collections = ContentCollection.getCollectionIds(request.ctx.organization,Permission.Read)
      itemList(q,f,c,sk,l,sort,collections,request.ctx.isLoggedIn)
  }
  private def itemList(q:Option[String],f: Option[String], c:String, sk:Int, l:Int, sort: Option[String], collections:Seq[ObjectId], isLoggedIn:Boolean):Result = {
    if (collections.nonEmpty){
      val queryResult:Either[SearchCancelled,MongoDBObject] = q.map(query => ItemSearch.toSearchObj(query,
        if (collections.size == 1) Some(MongoDBObject(Item.collectionId -> collections(0))) else Some(MongoDBObject(Item.collectionId -> MongoDBObject("$in" -> collections.map(_.toString))))
      )) match {
        case Some(result) => result
        case None => Right(MongoDBObject())
      }
      val fieldResult:Either[InternalError,SearchFields] = f.map(fields => ItemSearch.toFieldsObj(fields)) match {
        case Some(result) => result
        case None => Right(SearchFields(method = 1))
      }

      queryResult match {
        case Right(query) => fieldResult match {
          case Right(searchFields) => {
            if(c == "true"){
              val count = Item.find(query).count
              Ok(toJson(JsObject(Seq("count" -> JsNumber(count)))))
            }else{
              cleanDbFields(searchFields,isLoggedIn)
              val optitems:Either[InternalError,SalatMongoCursor[Item]] = sort.map(ItemSearch.toSortObj(_)) match {
                case Some(Right(sortField)) => Right(Item.find(query,searchFields.dbfields).sort(sortField).skip(sk).limit(l))
                case None => Right(Item.find(query,searchFields.dbfields).skip(sk).limit(l))
                case Some(Left(error)) => Left(error)
              }
              optitems match {
                case Right(items) => {
                  val itemViews:Seq[ItemView] = Utils.toSeq(items).map(ItemView(_,Some(searchFields)))
                  Ok(toJson(itemViews))
                }
                case Left(error) => BadRequest(toJson(ApiError.InvalidSort(error.clientOutput)))
              }
            }
            case _ => Some(enforcedQuery)
          }
          case Left(error) => BadRequest(toJson(ApiError.InvalidFields(error.clientOutput)))
        }
        case Left(sc) => sc.error match {
          case None => Ok(toJson(JsObject(Seq())))
          case Some(error) => BadRequest(toJson(ApiError.InvalidQuery(error.clientOutput)))
        }
      }
    }else Ok(toJson(JsObject(Seq())))
  }
  private def cleanDbFields(searchFields:SearchFields, isLoggedIn:Boolean, extraFields:Seq[String] = summaryFields) = {
    if(!isLoggedIn && searchFields.dbfields.isEmpty){
      extraFields.foreach(extraField =>
        searchFields.dbfields = searchFields.dbfields ++ MongoDBObject(extraField -> searchFields.method)
      )
    }
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    request =>
      if (Organization.getTree(request.ctx.organization).exists(_.id == orgId)) {
        val collections = ContentCollection.getCollectionIds(orgId,Permission.Read)
        itemList(q,f,c,sk,l,sort,collections,request.ctx.isLoggedIn)
      } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    request =>
      if (ContentCollection.isAuthorized(request.ctx.organization, collId, Permission.Read)) {
        itemList(q,f,c,sk,l,sort,Seq(collId),request.ctx.isLoggedIn)
      } else Unauthorized(toJson(ApiError.UnauthorizedOrganization))
  }


  /**
   * Returns an Item.  Only the default fields are rendered back.
   */
  def get(id: ObjectId) = ApiAction { request =>
      if(Content.isAuthorized(request.ctx.organization,id,Permission.Read)){
        val searchFields = SearchFields(method = 1)
        cleanDbFields(searchFields,request.ctx.isLoggedIn)
        val items = Item.find(MongoDBObject("_id" -> id),searchFields.dbfields)
        Ok(toJson(Utils.toSeq(items).head))
      }else Unauthorized(toJson(ApiError.UnauthorizedOrganization(Some("you do not have access to this item"))))
  }

  /**
   * Returns an Item with all its fields.
   *
   * @param id
   * @return
   */
  def getDetail(id: ObjectId) = ApiAction {
    request =>
      val detailsExcludeFields: Seq[String] = Seq(Item.data)
      if(Content.isAuthorized(request.ctx.organization,id,Permission.Read)){
        val searchFields = SearchFields(method = 0)
        cleanDbFields(searchFields,request.ctx.isLoggedIn,detailsExcludeFields)
        val items = Item.find(MongoDBObject("_id" -> id),searchFields.dbfields)
        Ok(toJson(Utils.toSeq(items).head))
      }else Unauthorized(toJson(ApiError.UnauthorizedOrganization(Some("you do not have access to this item"))))
  }

//  /**
//   * Helper method to retrieve Items from mongo.
//   *
//   * @param id
//   * @param fields
//   * @return
//   */
//  private def getWithFields(callerOrg: ObjectId, id: ObjectId, fields: Option[DBObject]): Result = {
//    fields.map(Item.collection.findOneByID(id, _)).getOrElse(Item.collection.findOneByID(id)) match {
//      case Some(o) => o.get(Item.collectionId) match {
//        case collId: String => if (Content.isCollectionAuthorized(callerOrg, collId, Permission.Read)) {
//          val i = grater[Item].asObject(o)
//          Ok(toJson(i))
//        } else {
//          Forbidden
//        }
//        case _ => Forbidden
//      }
//      case _ => NotFound("Item not found: " + id.toString)
//    }
//  }

  /**
   * Returns the raw content body for the item
   *
   * @param id
   * @return
   */
  def getData(id: ObjectId) = ApiAction {
    request =>
      Item.collection.findOneByID(id, MongoDBObject(Item.data -> 1, Item.collectionId -> 1)) match {
        case Some(o) => o.get(Item.collectionId) match {
          case collId: String => if (Content.isCollectionAuthorized(request.ctx.organization, collId, Permission.Read)) {
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
  def delete(id: ObjectId) = ApiAction {
    request =>
      Item.collection.findOneByID(id, MongoDBObject(Item.collectionId -> 1)) match {
        case Some(o) => o.get(Item.collectionId) match {
          case collId: String => if (Content.isCollectionAuthorized(request.ctx.organization, collId, Permission.Write)) {
            Content.moveToArchive(id) match {
              case Right(_) => Ok(com.mongodb.util.JSON.serialize(o))
              case Left(error) => InternalServerError(toJson(ApiError.Item.Delete(error.clientOutput)))
            }
          } else {
            Forbidden
          }
          case _ => Forbidden
        }
        case _ => NotFound
      }
  }

  private def cloneS3File(sourceFile: StoredFile, newId: String):String = {
    Log.d("Cloning " + sourceFile.storageKey + " to " + newId)
    val oldStorageKeyIdRemoved = sourceFile.storageKey.replaceAll("^[0-9a-fA-F]+/","")
    s3service.cloneFile(AMAZON_ASSETS_BUCKET, sourceFile.storageKey, newId+"/"+oldStorageKeyIdRemoved)
    newId+"/"+oldStorageKeyIdRemoved
  }

  private def cloneStoredFiles(oldItem: Item, newItem: Item): Boolean = {
    val newItemId = newItem.id.toString
    try {
      newItem.data.get.files.foreach {
        file => file match {
          case sf: StoredFile =>
            val newKey = cloneS3File(sf, newItemId)
            sf.storageKey = newKey
          case _ =>
        }
      }
      newItem.supportingMaterials.foreach {
        sm =>
          sm.files.filter(_.isInstanceOf[StoredFile]).foreach {
            file =>
              val sf = file.asInstanceOf[StoredFile]
              val newKey = cloneS3File(sf, newItemId)
              sf.storageKey = newKey
          }
      }
      Item.save(newItem)
      true
    } catch {
      case r: RuntimeException =>
        Log.e("Error cloning some of the S3 files: " + r.getMessage)
        Log.e(r.getStackTrace.mkString("\n"))
        false
    }

  }

  /**
   * Note: Have to call this 'cloneItem' instead of 'clone' as clone is a default
   * function.
   * @param id
   * @return
   */
  def cloneItem(id: ObjectId) = ApiAction {
    request =>
      findAndCheckAuthorization(request.ctx.organization, id, Permission.Write) match {
        case Left(e) => BadRequest(toJson(e))
        case Right(item) => {
          Item.cloneItem(item) match {
            case Some(clonedItem) =>
              cloneStoredFiles(item, clonedItem) match {
                case true => Ok(toJson(clonedItem))
                case false => BadRequest(toJson(ApiError.Item.Clone))
              }
            case _ => BadRequest(toJson(ApiError.Item.Clone))
          }
        }
      }
  }


  private def findAndCheckAuthorization(orgId: ObjectId, id: ObjectId, p: Permission): Either[ApiError, Item] = Item.findOneById(id) match {
    case Some(s) => Content.isCollectionAuthorized(orgId, s.collectionId, p) match {
      case true => Right(s)
      case false => Left(ApiError.CollectionUnauthorized)
    }
    case None => Left(ApiError.Item.NotFound)
  }

  def create = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) => {
          try {
            if ((json \ "id").asOpt[String].isDefined) {
              BadRequest(toJson(ApiError.IdNotNeeded))
            } else {
              val i: Item = fromJson[Item](json)
              if (i.collectionId.isEmpty && request.ctx.permission.has(Permission.Write)) {
                Organization.getDefaultCollection(request.ctx.organization) match {
                  case Right(default) => {
                    i.collectionId = default.id.toString;
                    Item.insert(i) match {
                      case Some(_) => Ok(toJson(i))
                      case None => InternalServerError(toJson(ApiError.CantSave))
                    }
                  }
                  case Left(error) => InternalServerError(toJson(ApiError.CantSave(error.clientOutput)))
                }
              } else if (Content.isCollectionAuthorized(request.ctx.organization, i.collectionId, Permission.Write)) {
                Item.insert(i) match {
                  case Some(_) => Ok(toJson(i))
                  case None => InternalServerError(toJson(ApiError.CantSave))
                }
              } else {
                Unauthorized(toJson(ApiError.CollectionUnauthorized))
              }
            }
          } catch {
            case parseEx: JSONParseException => BadRequest(toJson(ApiError.JsonExpected))
            case e: SalatInsertError => InternalServerError(toJson(ApiError.CantSave))
          }
        }
        case _ => BadRequest(toJson(ApiError.JsonExpected))
      }
  }

  def update(id: ObjectId) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, id, Permission.Write)) {
        request.body.asJson match {
          case Some(json) => {
            if ((json \ Item.id).asOpt[String].isDefined) {
              BadRequest(toJson(ApiError.IdNotNeeded))
            } else {
              try {
                val item = fromJson[Item](json)
                val fields = summaryFields.foldRight[MongoDBObject](MongoDBObject())((field,dbo) => dbo ++ MongoDBObject(field -> 1))
                Item.updateItem(id, item, if (request.ctx.isLoggedIn) None else Some(fields), request.ctx.organization) match {
                  case Right(i) => Ok(toJson(i))
                  case Left(error) => InternalServerError(toJson(ApiError.Item.Update(error.clientOutput)))
                }
              } catch {
                case e: JSONParseException => BadRequest(toJson(ApiError.JsonExpected))
                case e: JsonValidationException => BadRequest(toJson(ApiError.JsonExpected(Some(e.getMessage))))
              }
            }
          }
          case _ => BadRequest(toJson(ApiError.JsonExpected))
        }
      } else Forbidden
  }

  def getItemsInCollection(collId: ObjectId) = ApiAction {
    request =>
      NotImplemented
  }
}

object ItemApi extends api.v1.ItemApi(ConcreteS3Service)
