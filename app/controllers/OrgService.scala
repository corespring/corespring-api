package controllers.services

import models._
import controllers.auth.Permission
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import com.novus.salat._
import dao.SalatDAOUpdateError
import models.mongoContext._
import scala.Left
import scala.Some
import scala.Right
import api.ApiError

object OrgService {
  /**
   * get the path of the given organization in descending order. e.g. Massachusetts Board of Education -> Some Mass. District -> Some Mass. School
   * @param org: the organization in which to retrieve all parents from
   */
  def getPath(org: Organization): List[Organization] = {
    val c = Organization.find(MongoDBObject("_id" -> MongoDBObject("$in" -> org.path)))
    val orgList = c.toList
    c.close()
    orgList
  }

  /**
   * get immediate sub-nodes of given organization, exactly one depth greather than parent.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  def getChildren(parentId: ObjectId): List[Organization] = {
    val c = Organization.find(MongoDBObject(Organization.path -> parentId))

    val orgList = c.filter(o => if (o.path.size >= 2) o.path(o.path.size - 2) == parentId else false).toList
    c.close()
    orgList
  }

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  def getTree(parentId: ObjectId): List[Organization] = {
    val c = Organization.find(MongoDBObject(Organization.path -> parentId))
    val orgList = c.filter(_.id != parentId).toList
    c.close
    orgList
  }

  def isChild(parentId: ObjectId, childId: ObjectId): Boolean = {
    Organization.findOneById(childId) match {
      case Some(child) => {
        if (child.path.size >= 2) child.path(child.path.size - 2) == parentId else false
      }
      case None => false
    }
  }

  def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean = {
    Organization.findOne(MongoDBObject("_id" -> orgId,
      Organization.contentcolls -> MongoDBObject("$elemMatch" ->
        MongoDBObject(ContentCollRef.collId -> collRef.collId, ContentCollRef.pval -> collRef.pval)))).isDefined
  }

  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Either[ApiError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        Organization.update(MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false, false, Organization.collection.writeConcern)
        Right(collRef)
      } else {
        Left(ApiError(ApiError.IllegalState, "collection reference already exists"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(ApiError(ApiError.DatabaseError, e.getMessage))
    }
  }
}
