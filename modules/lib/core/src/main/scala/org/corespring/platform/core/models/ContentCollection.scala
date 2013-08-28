package org.corespring.platform.core.models

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.models.search.Searchable
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import scala.Left
import scala.Right
import scala.Some
import scalaz.{ Failure, Success, Validation }
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.common.log.ClassLogging
import com.novus.salat._
import com.novus.salat.dao._
import se.radley.plugin.salat._

/**
 * A ContentCollection
 */
case class ContentCollection(var name: String = "", var isPublic: Boolean = false, var id: ObjectId = new ObjectId()) {
  lazy val itemCount: Int = ItemServiceImpl.find(MongoDBObject("collectionId" -> id.toString)).count
}

object ContentCollection extends ModelCompanion[ContentCollection, ObjectId] with Searchable with ClassLogging {
  val name = "name"
  val isPublic = "isPublic"
  val DEFAULT = "default" //used as the value for name when the content collection is a default collection

  val collection = mongoCollection("contentcolls")

  import org.corespring.platform.core.models.mongoContext.context

  val dao = new SalatDAO[ContentCollection, ObjectId](collection = collection) {}

  def insertCollection(orgId: ObjectId, coll: ContentCollection, p: Permission): Either[InternalError, ContentCollection] = {
    //TODO: apply two-phase commit
    if (Play.isProd) coll.id = new ObjectId()
    try {
      super.insert(coll) match {
        case Some(_) => try {
          Organization.update(MongoDBObject("_id" -> orgId),
            MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> grater[ContentCollRef].asDBObject(new ContentCollRef(coll.id, p.value)))),
            false, false, Organization.collection.writeConcern)
          Right(coll)
        } catch {
          case e: SalatDAOUpdateError => Left(InternalError("failed to update organization with collection", e))
        }
        case None => Left(InternalError("failed to insert content collection"))
      }
    } catch {
      case e: SalatInsertError => Left(InternalError("failed to insert content collection", e))
    }
  }

  //TODO if public content collection, use two-phase commit and add possibility for rollback
  def updateCollection(coll: ContentCollection): Either[InternalError, ContentCollection] = {
    try {
      ContentCollection.update(MongoDBObject("_id" -> coll.id), coll, false, false, ContentCollection.collection.writeConcern)
      if (coll.isPublic) {
        Organization.update(MongoDBObject(),
          MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> coll.id)),
          false, false, Organization.defaultWriteConcern)
      }
      ContentCollection.findOneById(coll.id) match {
        case Some(coll) => Right(coll)
        case None => Left(InternalError("could not find the collection that was just updated"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("failed to update collection", e))
    }
  }

  lazy val archiveCollId: ObjectId = {
    val id = new ObjectId("500ecfc1036471f538f24bdc")
    ContentCollection.insert(ContentCollection("archiveColl", id = id))
    id
  }

  def delete(collId: ObjectId): Validation[InternalError, Unit] = {
    //todo: roll backs after detecting error in organization update
    try {
      ContentCollection.removeById(collId)
      Organization.find(MongoDBObject(Organization.contentcolls + "." + ContentCollRef.collectionId -> collId)).foldRight[Validation[InternalError, Unit]](Success(()))((org, result) => {
        if (result.isSuccess) {
          org.contentcolls = org.contentcolls.filter(_.collectionId != collId)
          try {
            Organization.update(MongoDBObject("_id" -> org.id), org, false, false, Organization.defaultWriteConcern)
            Success(())
          } catch {
            case e: SalatDAOUpdateError => Failure(InternalError(e.getMessage))
          }
        } else result
      })
    } catch {
      case e: SalatDAOUpdateError => Failure(InternalError("failed to transfer collection to archive", e))
      case e: SalatRemoveError => Failure(InternalError(e.getMessage))
    }
  }
  //  def moveToArchive(collId:ObjectId):Either[InternalError,Unit] = {
  //    //todo: roll backs after detecting error in organization update
  //    try{
  //      Content.collection.update(MongoDBObject(Content.collectionId -> collId), MongoDBObject("$set" -> MongoDBObject(Content.collectionId -> ContentCollection.archiveCollId.toString)),
  //        false, false, Content.collection.writeConcern)
  //      ContentCollection.removeById(collId)
  //      Organization.find(MongoDBObject(Organization.contentcolls+"."+ContentCollRef.collectionId -> collId)).foldRight[Either[InternalError,Unit]](Right(()))((org,result) => {
  //        if (result.isRight){
  //          org.contentcolls = org.contentcolls.filter(_.collectionId != collId)
  //          try {
  //            Organization.update(MongoDBObject("_id" -> org.id),org,false,false,Organization.defaultWriteConcern)
  //            Right(())
  //          }catch {
  //            case e:SalatDAOUpdateError => Left(InternalError(e.getMessage))
  //          }
  //        }else result
  //      })
  //    }catch{
  //      case e:SalatDAOUpdateError => Left(InternalError("failed to transfer collection to archive", e))
  //      case e:SalatRemoveError => Left(InternalError(e.getMessage))
  //    }
  //  }
  def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef] = {
    val cursor = if (deep) Organization.find(MongoDBObject(Organization.path -> orgId)) else Organization.find(MongoDBObject("_id" -> orgId)) //find the tree of the given organization
    var seqcollid: Seq[ContentCollRef] = cursor.foldRight[Seq[ContentCollRef]](Seq())((o, acc) => acc ++ o.contentcolls.filter(ccr => (ccr.pval & p.value) == p.value)) //filter the collections that don't have the given permission
    cursor.close()
    if (p == Permission.Read) {
      seqcollid = (seqcollid ++ getPublicCollections.map(c => ContentCollRef(c.id))).distinct
    }
    seqcollid
  }

  def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId] = {
    val cursor = if (deep) Organization.find(MongoDBObject(Organization.path -> orgId)) else Organization.find(MongoDBObject("_id" -> orgId)) //find the tree of the given organization
    var seqcollid: Seq[ObjectId] = cursor.foldRight[Seq[ObjectId]](Seq())((o, acc) => acc ++ o.contentcolls.filter(ccr => (ccr.pval & p.value) == p.value).map(_.collectionId)) //filter the collections that don't have the given permission
    cursor.close()
    if (p == Permission.Read) {
      seqcollid = (seqcollid ++ getPublicCollections.map(_.id)).distinct
    }
    seqcollid
  }

  def getPublicCollections: Seq[ContentCollection] = ContentCollection.find(MongoDBObject(isPublic -> true)).toSeq

  /**
   *
   * @param orgs contains a sequence of (organization id -> permission) tuples
   * @param collId
   * @return
   */
  def addOrganizations(orgs: Seq[(ObjectId, Permission)], collId: ObjectId): Either[InternalError, Unit] = {
    val errors = orgs.map(org => Organization.addCollection(org._1, collId, org._2)).filter(_.isLeft)
    if (errors.size > 0) Left(errors(0).left.get)
    else Right(())
  }

  /**
   * does the given organization have access to the given collection with given permissions?
   * @param orgId
   * @param collId
   */
  def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean = {
    val orgCollectionIds = getCollectionIds(orgId, p)
    val exists = orgCollectionIds.exists(_ == collId)
    if (!exists) {
      logger.debug(s"[isAuthorized] == false : orgId: $orgId, collection id: $collId isn't in: ${orgCollectionIds.mkString(",")}")
    }
    exists
  }

  implicit object CollectionWrites extends Writes[ContentCollection] {
    def writes(coll: ContentCollection): JsValue = {
      var list = List[(String, JsString)]()
      if (coll.name.nonEmpty) list = ("name" -> JsString(coll.name)) :: list
      list = ("id" -> JsString(coll.id.toString)) :: list
      JsObject(list)
    }
  }
  override val searchableFields = Seq(
    name)
}

case class CollectionExtraDetails(coll: ContentCollection, access: Long)
object CollectionExtraDetails {
  implicit object CCWPWrites extends Writes[CollectionExtraDetails] {
    def writes(c: CollectionExtraDetails): JsValue = {
      JsObject(Seq(
        "name" -> JsString(c.coll.name),
        "permission" -> JsString(Permission.toHumanReadable(c.access)),
        "itemCount" -> JsNumber(c.coll.itemCount),
        "isPublic" -> JsBoolean(c.coll.isPublic),
        "id" -> JsString(c.coll.id.toString)))
    }
  }
}
