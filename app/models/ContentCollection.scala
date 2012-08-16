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
import controllers.{Log, LogType, InternalError}
import scala.Left
import play.api.libs.json.JsString
import scala.Some
import scala.Right
import play.api.libs.json.JsObject
import play.api.Play.current
import play.api.Play
import controllers.auth.Permission

/**
 * A ContentCollection
 */
case class ContentCollection(var name: String, var isPrivate: Boolean = false, var id: ObjectId = new ObjectId())

object ContentCollection extends ModelCompanion[ContentCollection, ObjectId] {
  val name = "name"
  val isPrivate = "isPrivate"

  val collection = mongoCollection("contentcolls")
  val dao = new SalatDAO[ContentCollection, ObjectId](collection = collection) {}

  def insert(orgId: ObjectId, coll: ContentCollection): Either[InternalError, ContentCollection] = {
    if(Play.isProd) coll.id = new ObjectId()
    try {
      super.insert(coll) match   {
        case Some(_) => try {
          Organization.update(MongoDBObject("_id" -> orgId),
            MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> grater[ContentCollRef].asDBObject(new ContentCollRef(coll.id)))),
            false, false, Organization.collection.writeConcern)
          Right(coll)
        } catch {
          case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to update organization with collection")))
        }
        case None => Left(InternalError("failed to insert content collection",LogType.printFatal,true))
      }
    } catch {
      case e: SalatInsertError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to insert content collection")))
    }

  }

  def removeCollection(collId: ObjectId): Either[InternalError, Unit] = {
    ContentCollection.moveToArchive(collId) match {
      case Right(_) => try {
        //remove collection references from organizations
        Organization.update(MongoDBObject(Organization.contentcolls + "." + ContentCollRef.collId -> collId),
          MongoDBObject("$pull" -> MongoDBObject(Organization.contentcolls -> MongoDBObject(ContentCollRef.collId -> collId))),
          false, false, Organization.collection.writeConcern)
        //finally delete
        ContentCollection.removeById(collId, ContentCollection.collection.writeConcern)
        Right(())
      } catch {
        case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to update corresponding organizations")))
        case e: SalatRemoveError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to remove")))
      }
      case Left(e) => Left(e)
    }

  }

  def updateCollection(coll: ContentCollection): Either[InternalError, ContentCollection] = {
    try {
      ContentCollection.update(MongoDBObject("_id" -> coll.id), MongoDBObject("$set" ->
        MongoDBObject(ContentCollection.name -> coll.name)), false, false, ContentCollection.collection.writeConcern)
      ContentCollection.findOneById(coll.id) match {
        case Some(coll) => Right(coll)
        case None => Left(InternalError("could not find the collection that was just updated",LogType.printFatal,true))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to update collection")))
    }
  }
  lazy val archiveCollId: ObjectId = ContentCollection.findOneById(new ObjectId("500ecfc1036471f538f24bdc")) match {
    case Some(coll) => coll.id
    case None => ContentCollection.insert(ContentCollection("archiveColl", id = new ObjectId("500ecfc1036471f538f24bdc"))) match {
      case Some(collId) => collId
      case None => throw new RuntimeException("could not create new archive collection")
    }
  }
  def moveToArchive(collId:ObjectId):Either[InternalError,Unit] = {
    try{
      Content.collection.update(MongoDBObject(Content.collId -> collId), MongoDBObject("$set" -> MongoDBObject(Content.collId -> ContentCollection.archiveCollId)),
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
    val errors = orgIds.map(oid => Organization.addCollection(oid, collId, p)).filter(_.isLeft)
    if (errors.size > 0) Left(errors(0).left.get)
    else Right(())
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
