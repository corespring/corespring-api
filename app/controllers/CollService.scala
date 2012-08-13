package controllers

import controllers.auth.Permission
import org.bson.types.ObjectId
import models._
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import com.novus.salat._
import dao.SalatDAOUpdateError
import scala.Left
import scala.Some
import scala.Right
import controllers.services.OrgService
import models.mongoContext._
import scala.Left
import scala.Some
import scala.Right


object CollService {
  lazy val archiveCollId: ObjectId = ContentCollection.findOneById(new ObjectId("500ecfc1036471f538f24bdc")) match {
    case Some(coll) => coll.id
    case None => ContentCollection.insert(ContentCollection("archiveColl", id = new ObjectId("500ecfc1036471f538f24bdc"))) match {
      case Some(collId) => collId
      case None => throw new RuntimeException("could not create new archive collection")
    }
  }
  def moveToArchive(collId:ObjectId):Either[InternalError,Unit] = {
    try{
    Content.collection.update(MongoDBObject(Content.collId -> collId), MongoDBObject("$set" -> MongoDBObject(Content.collId -> CollService.archiveCollId)),
      false, false, Content.collection.writeConcern)
      Right(())
    }catch{
      case e:SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to transfer collection to archive")))
    }
  }
  def getCollections(orgId: ObjectId, p:Permission): Either[InternalError, Seq[ContentCollection]] = {
    val cursor = Organization.find(MongoDBObject(Organization.path -> orgId))   //find the tree of the given organization
    val seqoptcoll:Seq[Option[ContentCollection]] = cursor.
        foldRight[Seq[ContentCollRef]](Seq())((o,acc) => acc ++ o.contentcolls.filter(ccr => (ccr.pval&p.value) == p.value)). //filter the collections that don't have the given permission
        foldRight[Seq[Option[ContentCollection]]](Seq())((ccr,acc) => {acc :+ ContentCollection.findOneById(ccr.collId)}) //retrieve the collections
    val seqcoll:Seq[ContentCollection] = seqoptcoll.filter(_.isDefined).map(_.get) //filter any collections that were not retrieved
    if (seqoptcoll.size > seqcoll.size) Log.f("CollService.getCollections: there are collections referenced by organizations that don't exist in the database. structure compromised")
    cursor.close()
    Right(seqcoll)
  }

  def addOrganizations(orgIds: Seq[ObjectId], collId: ObjectId, p: Permission): Either[InternalError, Unit] = {
    val errors = orgIds.map(oid => OrgService.addCollection(oid, collId, p)).filter(_.isLeft)
    if (errors.size > 0) Left(errors(0).left.get)
    else Right(())
  }
}
