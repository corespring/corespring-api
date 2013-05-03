package models

import se.radley.plugin.salat._
import play.api.libs.json._
import play.api.libs.json.JsObject
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatRemoveError, SalatDAOUpdateError, ModelCompanion, SalatDAO}
import controllers.auth.Permission
import play.api.Play.current
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import models.mongoContext._
import play.api.Play
import controllers.{Utils, LogType, InternalError}
import com.novus.salat._
import dao.SalatDAOUpdateError
import dao.SalatRemoveError
import scala.Left
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import scala.Right
import play.api.libs.json.JsObject
import search.Searchable

case class Organization(var name: String = "",
                        var path: Seq[ObjectId] = Seq(),
                        var contentcolls: Seq[ContentCollRef] = Seq(),
                        var id: ObjectId = new ObjectId()){

  def this() = this("")
}

object Organization extends ModelCompanion[Organization,ObjectId] with Searchable{

  val name: String = "name"
  val path: String = "path"
  val contentcolls: String = "contentcolls"
  val id = "id"

  val collection = mongoCollection("orgs")
  val dao = new SalatDAO[Organization, ObjectId](collection = collection) {}

  def apply(): Organization = new Organization();


  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Either[InternalError, Organization] = {
    if(Play.isProd) org.id = new ObjectId()
    optParentId match {
      case Some(parentId) => {
        findOneById(parentId) match {
          case Some(parent) => {
            org.path = Seq(org.id) ++ parent.path
            org.contentcolls = org.contentcolls ++ ContentCollection.getPublicCollections.map(cc => ContentCollRef(cc.id,Permission.Read.value))
            insert(org) match {
              case Some(id) => Right(org)
              case None => Left(InternalError("error inserting organization",LogType.printFatal,true))
            }
          }
          case None => Left(InternalError("could not find parent given id",LogType.printError,true))
        }
      }
      case None => {
        org.path = Seq(org.id)
        org.contentcolls = org.contentcolls ++ ContentCollection.getPublicCollections.map(cc => ContentCollRef(cc.id,Permission.Read.value))
        insert(org) match {
          case Some(id) => Right(org)
          case None => Left(InternalError("error inserting organization",LogType.printFatal,true))
        }
      }
    }
  }

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  def delete(orgId: ObjectId): Either[InternalError, Unit] = {
    try {
      remove(MongoDBObject(Organization.path -> orgId))
      Right(())
    } catch {
      case e:SalatRemoveError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to destroy organization tree")))
    }
  }

  def updateOrganization(org: Organization): Either[InternalError, Organization] = {
    try {
      Organization.update(MongoDBObject("_id" -> org.id), MongoDBObject("$set" -> MongoDBObject(name -> org.name)),
        false, false, Organization.collection.writeConcern)
      Organization.findOneById(org.id) match {
        case Some(org) => Right(org)
        case None => Left(InternalError("could not find organization that was just modified",LogType.printFatal,true))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("unable to update organization")))
    }
  }

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  def getTree(parentId: ObjectId): Seq[Organization] = {
    val c = Organization.find(MongoDBObject(Organization.path -> parentId))
    Utils.toSeq(c)
  }

  def isChild(parentId: ObjectId, childId: ObjectId): Boolean = {
    Organization.findOneById(childId) match {
      case Some(child) => {
        if (child.path.size >= 2) child.path(1) == parentId else false
      }
      case None => false
    }
  }

  def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean = {
    Organization.findOne(MongoDBObject("_id" -> orgId,
      Organization.contentcolls -> MongoDBObject("$elemMatch" ->
        MongoDBObject(ContentCollRef.collectionId -> collRef.collectionId, ContentCollRef.pval -> collRef.pval)))).isDefined
  }
  def removeCollection(orgId:ObjectId, collId: ObjectId):Either[InternalError,Unit] = {
    Organization.findOneById(orgId) match {
      case Some(org) => {
        org.contentcolls = org.contentcolls.filter(_.collectionId != collId)
        try {
          Organization.update(MongoDBObject("_id" -> orgId),org,false,false,Organization.defaultWriteConcern)
          Right(())
        }catch {
          case e:SalatDAOUpdateError => Left(InternalError(e.getMessage))
        }
      }
      case None => Left(InternalError("could not find organization",addMessageToClientOutput = true))
    }
  }

  def getPermissions(orgId: ObjectId, collId: ObjectId):Permission = {
    getTree(orgId).foldRight[Permission](Permission.None)((o,p) => {
      o.contentcolls.find(_.collectionId == collId) match {
        case Some(ccr) => Permission.fromLong(ccr.pval).getOrElse(p)
        case None => p
      }
    })
  }
  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Either[InternalError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        Organization.update(MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false, false, Organization.collection.writeConcern)
        Right(collRef)
      } else {
        Left(InternalError("collection reference already exists",LogType.printError,true))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal))
    }
  }
  def getDefaultCollection(orgId: ObjectId):Either[InternalError,ContentCollection] = {
    val collections = ContentCollection.getCollectionIds(orgId,Permission.Write,false);
    if (collections.isEmpty){
      ContentCollection.insertCollection(orgId,ContentCollection(ContentCollection.DEFAULT),Permission.Write);
    }else{
      ContentCollection.findOne(
        MongoDBObject("_id" -> MongoDBObject("$in" -> collections), ContentCollection.name -> ContentCollection.DEFAULT)
      ) match {
        case Some(default) => Right(default)
        case None =>
          ContentCollection.insertCollection(orgId,ContentCollection(ContentCollection.DEFAULT),Permission.Write);
      }
    }
  }
  implicit object OrganizationWrites extends Writes[Organization] {
    def writes(org: Organization) = {
      var list = List[(String, JsValue)]()
      if ( org.path.nonEmpty ) list = ("path" -> JsArray(org.path.map(c => JsString(c.toString)).toSeq)) :: list
      if ( org.name.nonEmpty ) list = ("name" -> JsString(org.name)) :: list
      list = ("id" -> JsString(org.id.toString)) :: list
      JsObject(list)
    }
  }
  override val searchableFields = Seq(
    name
  )
}

case class ContentCollRef(var collectionId: ObjectId, var pval: Long = Permission.Read.value)

object ContentCollRef {
  val pval: String = "pval"
  val collectionId: String = "collectionId"
}



