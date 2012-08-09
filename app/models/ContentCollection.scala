package models

import org.bson.types.ObjectId
import mongoContext._
import se.radley.plugin.salat._
import play.api.libs.json._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import dao._
import dao.SalatDAOUpdateError
import dao.SalatInsertError
import dao.SalatRemoveError
import dao.SalatSaveError
import controllers.CollService
import api.ApiError
import controllers.services.OrgService
import controllers.auth.Permission
import scala.Left
import play.api.libs.json.JsString
import scala.Some
import scala.Right
import play.api.libs.json.JsObject
import play.api.Play.current
import play.api.Play

/**
 * A ContentCollection
 */
case class ContentCollection(var name: String, var isPrivate: Boolean = false, var id: ObjectId = new ObjectId())

object ContentCollection extends ModelCompanion[ContentCollection, ObjectId] {
  val name = "name"
  val isPrivate = "isPrivate"

  val collection = mongoCollection("contentcolls")
  val dao = new SalatDAO[ContentCollection, ObjectId](collection = collection) {}

  def insert(orgId: ObjectId, coll: ContentCollection): Either[ApiError, ContentCollection] = {
    if(Play.isProd) coll.id = new ObjectId()
    try {
      super.insert(coll) match   {
        case Some(_) => try {
          Organization.update(MongoDBObject("_id" -> orgId),
            MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> grater[ContentCollRef].asDBObject(new ContentCollRef(coll.id)))),
            false, false, Organization.collection.writeConcern)
          Right(coll)
        } catch {
          case e: SalatDAOUpdateError => Left(ApiError(ApiError.DatabaseError, "failed to add collection to organization"))
        }
        case None => Left(ApiError(ApiError.DatabaseError, "failed to insert content collection"))
      }
    } catch {
      case e: SalatInsertError => Left(ApiError(ApiError.DatabaseError, "failed to insert content collection"))
    }

  }

  def removeCollection(collId: ObjectId): Either[ApiError, Unit] = {
    try {
      //move collection's content to archive collection
      Content.collection.update(MongoDBObject(Content.collId -> collId), MongoDBObject("$set" -> MongoDBObject(Content.collId -> CollService.archiveCollId)),
        false, false, Content.collection.writeConcern)
      //remove collection references from organizations
      Organization.update(MongoDBObject(Organization.contentcolls + "." + ContentCollRef.collId -> collId),
        MongoDBObject("$pull" -> MongoDBObject(Organization.contentcolls -> MongoDBObject(ContentCollRef.collId -> collId))),
        false, false, Organization.collection.writeConcern)
      //finally delete
      ContentCollection.removeById(collId, ContentCollection.collection.writeConcern)
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(ApiError(ApiError.DatabaseError, e.getMessage))
      case e: SalatRemoveError => Left(ApiError(ApiError.DatabaseError, e.getMessage))
    }
  }

  def updateCollection(coll: ContentCollection): Either[ApiError, ContentCollection] = {
    try {
      ContentCollection.update(MongoDBObject("_id" -> coll.id), MongoDBObject("$set" ->
        MongoDBObject(ContentCollection.name -> coll.name)), false, false, ContentCollection.collection.writeConcern)
      ContentCollection.findOneById(coll.id) match {
        case Some(coll) => Right(coll)
        case None => Left(ApiError(ApiError.IllegalState, "could not find the collection that was just updated"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(ApiError(ApiError.DatabaseError, e.getMessage))
    }
  }

  implicit object CollectionWrites extends Writes[ContentCollection] {
    def writes(coll: ContentCollection) = {
      JsObject(
        List(
          "id" -> JsString(coll.id.toString),
          "name" -> JsString(coll.name)
        )
      )
    }
  }

}
