package controllers

import auth.Permission
import org.bson.types.ObjectId
import models.{ContentCollRef, UserOrg, Organization, ContentCollection}
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import com.novus.salat._
import scala.Left
import scala.Some
import scala.Right
import controllers.services.OrgService
import models.mongoContext._


object CollService {
  lazy val archiveCollId: ObjectId = ContentCollection.findOne(MongoDBObject(ContentCollection.name -> "archiveColl")).
    getOrElse(throw new RuntimeException("no archive collection establisthed")).id

  def getCollections(orgId: ObjectId, p:Permission): Either[ApiError, Seq[ContentCollection]] = {
    val seqoptcoll:Seq[Option[ContentCollection]] = Organization.find(MongoDBObject(Organization.path -> orgId)).     //find the tree of the given organization
        foldRight[Seq[ContentCollRef]](Seq())((o,acc) => acc ++ o.contentcolls.filter(ccr => (ccr.pval&p.value) == p.value)). //filter the collections that don't have the given permission
        foldRight[Seq[Option[ContentCollection]]](Seq())((ccr,acc) => acc :+ ContentCollection.findOneById(ccr.collId)) //retrieve the collections
    val seqcoll:Seq[ContentCollection] = seqoptcoll.filter(_.isDefined).map(_.get) //filter any collections that were not retrieved
    if (seqoptcoll.size > seqcoll.size) Log.f("CollService.getCollections: there are collections referenced by organizations that don't exist in the database. structure compromised")
    Right(seqcoll)
  }

  def addOrganizations(orgIds: Seq[ObjectId], collId: ObjectId, p: Permission): Either[ApiError, Unit] = {
    val errors = orgIds.map(oid => OrgService.addCollection(oid, collId, p)).filter(_.isLeft)
    if (errors.size > 0) Left(errors(0).left.get)
    else Right(())
  }
}
