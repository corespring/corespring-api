package controllers

import auth.Permission
import org.bson.types.ObjectId
import models.{UserOrg, Organization, ContentCollection}
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import com.novus.salat._
import scala.Left
import scala.Some
import scala.Right
import controllers.services.OrgService
import models.mongoContext._

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/7/12
 * Time: 9:45 AM
 * To change this template use File | Settings | File Templates.
 */

object CollService {
  lazy val archiveCollId:ObjectId = ContentCollection.findOne(MongoDBObject(ContentCollection.name -> "archiveColl")).
    getOrElse(throw new RuntimeException("no archive collection establisthed")).id

  def getCollections(orgId:ObjectId, optPermission:Option[Permission]):Either[ApiError,Seq[ContentCollection]] = {
    optPermission match {
      case Some(p) => Organization.findOneById(orgId) match {
        case Some(org) => Right(ContentCollection.find(MongoDBObject("_id" -> MongoDBObject("$in" -> grater[UserOrg].asDBObject(UserOrg(orgId,p.value))))).toSeq)
        case None => Left(ApiError(ApiError.NotFound," could not find organization with given id: "+orgId.toString))
      }
      case None => Organization.findOneById(orgId) match {
        case Some(org) => Right(ContentCollection.find(MongoDBObject("_id" -> MongoDBObject("$in" -> org.contentcolls.map(_.collId)))).toSeq)
        case None => Left(ApiError(ApiError.NotFound," could not find organization with given id: "+orgId.toString))
      }
    }
  }
  def addOrganizations(orgIds:Seq[ObjectId], collId:ObjectId, p:Permission):Either[ApiError,Unit] = {
    val errors = orgIds.map(oid => OrgService.addCollection(oid,collId,p)).filter(_.isLeft)
    if (errors.size > 0) Left(errors(0).left.get)
    else Right(())
  }
}
