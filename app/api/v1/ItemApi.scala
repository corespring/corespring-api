package api.v1

import controllers.auth.{ApiRequest, Permission, BaseApi}
import api.ApiError
import models._
import com.mongodb.util.JSONParseException
import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.{SalatDAOUpdateError}
import dao.{SalatMongoCursor, SalatInsertError}
import item.resource.StoredFile
import play.api.templates.Xml
import play.api.mvc.Result
import play.api.libs.json.Json._
import models.mongoContext._
import controllers._
import play.api.libs.json._
import search.{SearchFields, SearchCancelled, ItemSearch}
import models.json.ItemView
import item.{Version}
import scala.Left
import play.api.libs.json.JsArray
import scala.Some
import play.api.libs.json.JsNumber
import controllers.InternalError
import scala.Right
import play.api.libs.json.JsObject
import com.mongodb.WriteResult
import com.typesafe.config.ConfigFactory
import item.{Content, Item, Alignments, TaskInfo}

/**
 * Items API
 */
class ItemApi(s3service:S3Service) extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  val dbsummaryFields = Seq(Item.collectionId,Item.taskInfo,Item.otherAlignments,Item.standards,Item.contributorDetails)
  val jssummaryFields: Seq[String] = Seq("id",
    Item.collectionId,
    TaskInfo.Keys.gradeLevel,
    TaskInfo.Keys.itemType,
    Alignments.Keys.keySkills,
    Item.primarySubject,
    Item.relatedSubject,
    Item.standards,
    Item.author,
    TaskInfo.Keys.title)

  private def count(c: String): Boolean = "true".equalsIgnoreCase(c)

  /**
   * List query implementation for Items
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      val collections = ContentCollection.getCollectionIds(request.ctx.organization,Permission.Read)
      itemList(q,f,c,sk,l,sort,collections)
  }
  private def itemList[A](q:Option[String],f: Option[String], c:String, sk:Int, l:Int, sort: Option[String], collections:Seq[ObjectId], current:Boolean = true)(implicit request:ApiRequest[A]):Result = {
    val parseCollectionIds:(AnyRef)=>Either[InternalError,AnyRef] = (value:AnyRef) => {
      value match {
        case dbo:BasicDBObject => dbo.toSeq.headOption match {
          case Some((key,dblist)) => if(key == "$in") {
            if(dblist.isInstanceOf[BasicDBList]){
              try{
                if(dblist.asInstanceOf[BasicDBList].toArray.forall(coll => ContentCollection.isAuthorized(request.ctx.organization,new ObjectId(coll.toString),Permission.Read)))
                  Right(value)
                else Left(InternalError("attempted to access a collection that you are not authorized to",addMessageToClientOutput = true))
              } catch {
                case e:IllegalArgumentException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse collectionId into an object id")))
              }
            }else Left(InternalError("invalid value for collectionId key. could not cast to array",addMessageToClientOutput = true))
          } else Left(InternalError("can only use $in special operator when querying on collectionId",addMessageToClientOutput = true))
          case None => Left(InternalError("empty db object as value of collectionId key",addMessageToClientOutput = true))
        }
        case _ => Left(InternalError("invalid value for collectionId",addMessageToClientOutput = true))
      }
    }
    if (collections.nonEmpty){
      val initSearch:MongoDBObject = if (collections.size == 1) MongoDBObject(Item.collectionId -> collections(0).toString) else MongoDBObject(Item.collectionId -> MongoDBObject("$in" -> collections.map(_.toString)))

      val queryResult:Either[SearchCancelled,MongoDBObject] = q.map(query => ItemSearch.toSearchObj(query,
        Some(initSearch),
        Map(Item.collectionId -> parseCollectionIds)
      )) match {
        case Some(result) => result
        case None => Right(initSearch)
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
              cleanDbFields(searchFields,request.ctx.isLoggedIn)
              val optitems:Either[InternalError,SalatMongoCursor[Item]] = sort.map(ItemSearch.toSortObj(_)) match {
                case Some(Right(sortField)) => Right(Item.find(query,searchFields.dbfields).sort(sortField).skip(sk).limit(l))
                case None => Right(Item.find(query,searchFields.dbfields).skip(sk).limit(l))
                case Some(Left(error)) => Left(error)
              }
              optitems match {
                case Right(items) => {
                  val itemViews:Seq[ItemView] = Utils.toSeq(items).
                    filter(i => if(current) i.version.map(_.current).getOrElse(true) else true).
                    map(ItemView(_,Some(searchFields)))
                  Ok(toJson(itemViews))
                }
                case Left(error) => BadRequest(toJson(ApiError.InvalidSort(error.clientOutput)))
              }
            }
          }
          case Left(error) => BadRequest(toJson(ApiError.InvalidFields(error.clientOutput)))
        }
        case Left(sc) => sc.error match {
          case None => Ok(JsArray(Seq()))
          case Some(error) => BadRequest(toJson(ApiError.InvalidQuery(error.clientOutput)))
        }
      }
    }else Ok(JsArray(Seq()))
  }

  private def cleanDbFields(searchFields:SearchFields, isLoggedIn:Boolean, dbExtraFields:Seq[String] = dbsummaryFields, jsExtraFields:Seq[String] = jssummaryFields) = {
    if(!isLoggedIn && searchFields.dbfields.isEmpty){
      dbExtraFields.foreach(extraField =>
        searchFields.dbfields = searchFields.dbfields ++ MongoDBObject(extraField -> searchFields.method)
      )
      jsExtraFields.foreach(extraField =>
        searchFields.jsfields = searchFields.jsfields :+ extraField
      )
    }
    if(searchFields.method == 1 && searchFields.dbfields.nonEmpty) searchFields.dbfields = searchFields.dbfields ++ MongoDBObject(Item.version -> 1)
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (Organization.getTree(request.ctx.organization).exists(_.id == orgId)) {
        val collections = ContentCollection.getCollectionIds(orgId,Permission.Read)
        itemList(q,f,c,sk,l,sort,collections)
      } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (ContentCollection.isAuthorized(request.ctx.organization, collId, Permission.Read)) {
        itemList(q,f,c,sk,l,sort,Seq(collId))
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
                val dbfields = dbsummaryFields.foldRight[MongoDBObject](MongoDBObject())((field,dbo) => dbo ++ MongoDBObject(field -> 1))
                Item.updateItem(id, item, if (request.ctx.isLoggedIn) None else Some(dbfields), request.ctx.organization) match {
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

  def cloneAndIncrement(itemId:ObjectId) = ApiAction {request =>
    if(Content.isAuthorized(request.ctx.organization,itemId,Permission.Read)){
      Item.findOneById(itemId) match {
        case Some(item) => {
          //TODO: allow for rollback of item if storing files fails or second update fails
          Item.cloneItem(item) match {
            case Some(clonedItem) => {
              cloneStoredFiles(item, clonedItem) match {
                case true => try{
                  item.version match {
                    case Some(ver) => Item.update(MongoDBObject("_id" -> item.id),
                      MongoDBObject("$set" -> MongoDBObject(Item.version+"."+Version.current -> false)),
                      false,false,Item.defaultWriteConcern)
                    case None => {
                      val version = Version(item.id,0,false)
                      Item.update(MongoDBObject("_id" -> item.id),
                        MongoDBObject("$set" -> MongoDBObject(Item.version -> grater[Version].asDBObject(version))),
                        false,false,Item.defaultWriteConcern)
                      item.version = Some(version)
                    }
                  }
                  val currentVersion = Version(item.version.get.root,item.version.get.rev+1,true)
                  Item.update(MongoDBObject("_id" -> clonedItem.id),
                    MongoDBObject("$set" -> MongoDBObject(Item.version -> grater[Version].asDBObject(currentVersion))),
                    false,false,Item.defaultWriteConcern)
                  Ok(Json.toJson(ItemView(clonedItem,None)))
                } catch {
                  case e:SalatDAOUpdateError => InternalServerError(Json.toJson(ApiError.Item.Clone(Some("could not update version"))))
                }
                case false => BadRequest(toJson(ApiError.Item.Clone))
              }
            }
            case _ => BadRequest(toJson(ApiError.Item.Clone))
          }
        }
        case None => throw new RuntimeException("a item that was authorized does not exist")
      }
    }else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  def increment(itemId: ObjectId) = ApiAction {request =>
    if(Content.isAuthorized(request.ctx.organization,itemId,Permission.Read)){
      request.body.asJson match {
        case Some(json) => {
          if ((json \ Item.id).asOpt[String].isDefined) {
            BadRequest(toJson(ApiError.IdNotNeeded))
          } else {
            try {
              val item = fromJson[Item](json)
              //TODO: provide ability for rollbacks if insert fails
              Item.findOneById(itemId) match {
                case Some(olditem) => {
                  olditem.version match {
                    case Some(ver) => Item.update(MongoDBObject("_id" -> olditem.id),
                      MongoDBObject("$set" -> MongoDBObject(Item.version+"."+Version.current -> false)),
                      false,false,Item.defaultWriteConcern)
                    case None => {
                      val version = Version(olditem.id,0,false)
                      Item.update(MongoDBObject("_id" -> olditem.id),
                        MongoDBObject("$set" -> MongoDBObject(Item.version -> grater[Version].asDBObject(version))),
                        false,false,Item.defaultWriteConcern)
                      olditem.version = Some(version)
                    }
                  }
                  item.version = Some(Version(olditem.version.get.root,olditem.version.get.rev+1,true))
                  val dbolditem = grater[Item].asDBObject(olditem)
                  val dbitem = grater[Item].asDBObject(item)
                  val newitem = grater[Item].asObject(dbolditem ++ dbitem)
                  Item.insert(newitem) match {
                    case Some(id) => Ok(toJson(newitem))
                    case None => InternalServerError(JsObject(Seq("message" -> JsString("a database error occurred when attempting to insert the new item revision"))))
                  }
                }
                case None => throw new RuntimeException("item could not be found after it was authorized")
              }
            } catch {
              case e: SalatDAOUpdateError => InternalServerError(JsObject(Seq("message" -> JsString("a database error occurred when attempting to update the revision number of the item"))))
              case e: JSONParseException => BadRequest(toJson(ApiError.JsonExpected))
              case e: JsonValidationException => BadRequest(toJson(ApiError.JsonExpected(Some(e.getMessage))))
            }
          }
        }
        case None => BadRequest(JsObject(Seq("message" -> JsString("required JSON item in post data. If you wish to clone item and increment, GET"))))
      }
    }else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
  }


  /**
   * returns the most recent revision of the item referred to by id
   * @param id
   * @return
   */
  def getCurrent(id: ObjectId) = ApiAction { request =>
    if(Content.isAuthorized(request.ctx.organization,id,Permission.Read)){
      val searchFields = SearchFields(method = 1)
      cleanDbFields(searchFields,request.ctx.isLoggedIn)
      val baseItem = Item.findOne(MongoDBObject("_id" -> id)).get
      baseItem.version match {
        case Some(version) => Item.findOne(MongoDBObject(Item.version+"."+Version.root -> version.root, Item.version+"."+Version.current -> true)) match {
          case Some(currentItem) => Ok(Json.toJson(ItemView(currentItem,None)))
          case None => InternalServerError(JsObject(Seq("message" -> JsString("there is no version of this item that is visible"))))
        }
        case None => {
          baseItem.version = Some(Version(baseItem.id,0,true))
          Ok(Json.toJson(ItemView(baseItem,None)))
        }
      }
    }else Unauthorized(toJson(ApiError.UnauthorizedOrganization(Some("you do not have access to this item"))))
  }

}

object ItemApi extends api.v1.ItemApi(ConcreteS3Service)
