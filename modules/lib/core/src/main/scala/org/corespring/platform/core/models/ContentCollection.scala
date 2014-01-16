package org.corespring.platform.core.models

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.models.search.{SearchCancelled, ItemSearch, Searchable}
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
import com.mongodb.casbah.Imports._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.Item.Keys._
import org.corespring.platform.core.models.search.SearchCancelled
import com.novus.salat.dao.SalatInsertError
import com.novus.salat.dao.SalatRemoveError
import scalaz.Failure
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import play.api.libs.json.JsNumber
import com.novus.salat.dao.SalatDAOUpdateError
import scalaz.Success
import play.api.libs.json.JsObject

/**
 * A ContentCollection
 * ownerOrgId is not a one-to-many with organization. Collections may be in multiple orgs, but they have one 'owner'
 *
 */
case class ContentCollection(
  var name: String = "",
  var ownerOrgId: String = "",
  var isPublic: Boolean = false,
  var id: ObjectId = new ObjectId()) {

  lazy val itemCount: Int = ItemServiceImpl.find(MongoDBObject("collectionId" -> id.toString)).count
}

object ContentCollection extends ModelCompanion[ContentCollection, ObjectId] with Searchable with ClassLogging {
  val name = "name"
  val isPublic = "isPublic"
  val ownerOrgId = "ownerOrgId"
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
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   *
   * @param orgId
   * @param items
   * @param collId
   * @return
   */
  def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Either[InternalError, Seq[VersionedId[ObjectId]]] = {
    if (isAuthorized(orgId, collId, Permission.Write)) {
      val oids = items.map(i => i.id)
      val query = MongoDBObject("_id._id" -> MongoDBObject("$in" -> oids))

      // get a list of any items that were not authorized to be added
      val itemsNotAuthorized = ItemServiceImpl.find(query).filterNot(item => {
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val collectionsToAuth = Seq(item.collectionId) ++ item.sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.filter(collectionId => isAuthorized(orgId, new ObjectId(collectionId), Permission.Read))
        collectionsAuthorized.size > 0
      })
      if (itemsNotAuthorized.size <= 0) {
        // add collection id to item.sharedinCollections unless the collection is the owner collection for item
        val savedUnsavedItems = items.partition(item => {
          try {
            ItemServiceImpl.findOneById(item) match {
              case Some(itemObj) if (collId.equals(itemObj)) => true
              case _ =>
                ItemServiceImpl.saveUsingDbo(item, MongoDBObject("$addToSet" -> MongoDBObject(Item.Keys.sharedInCollections -> collId)) ,false)
                true
            }
          } catch {
            case e: SalatDAOUpdateError => false
        }
        })
        if (savedUnsavedItems._2.size > 0) {
          logger.debug(s"[addItems] failed to add items: " + savedUnsavedItems._2.map(_.id + " ").toString)
          Left(InternalError("failed to add items"))
        } else {
          logger.debug(s"[addItems] added items: " + savedUnsavedItems._1.map(_.id + " ").toString)
          Right(savedUnsavedItems._1)
        }

      } else {
        Left(InternalError("items failed auth: " + itemsNotAuthorized.map(_.id)))
      }
    } else {
      Left(InternalError("organization does not have write permission on collection"))
    }

  }

  /**
   * Share the items returned by the query with the specified collection.
   *
   * @param orgId
   * @param query
   * @param collId
   * @return
   */
  def shareItemsMatchingQuery(orgId: ObjectId, query: String, collId: ObjectId): Either[InternalError, Seq[VersionedId[ObjectId]]] = {
    val acessibleCollections = ContentCollection.getCollectionIds(orgId, Permission.Read)
    val collectionsQuery = ItemServiceImpl.createDefaultCollectionsQuery(acessibleCollections)
    val parsedQuery: Either[SearchCancelled, MongoDBObject] = ItemSearch.toSearchObj(query, Some(collectionsQuery) )

    parsedQuery match {
      case Right(searchQry) =>
        val cursor = ItemServiceImpl.find(searchQry, MongoDBObject("_id" -> 1))
        val ids = cursor.map(item => item.id)
        shareItems(orgId,ids.toSeq, collId)
      case Left(sc) => sc.error match {
        case None => Right(Seq())
        case Some(error) => Left(InternalError(error.clientOutput.getOrElse("error processing search")))
      }
    }
  }


    /**
   * Unshare the specified items from the specified collections
   *
   * @param orgId
   * @param items - sequence of items to be unshared from
   * @param collIds - sequence of collections to have the items removed from
   * @return
   */
  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Either[InternalError, Seq[VersionedId[ObjectId]]] = {
    // make sure org has auth for all the collIds
    val authorizedCollIds = collIds.filter(id => isAuthorized(orgId, id, Permission.Write))
    if (authorizedCollIds.size != collIds.size) {
      Left(InternalError("authorization failed on collection(s)"))
    } else {
      val failedItems = items.filterNot(item => {
        try {
          ItemServiceImpl.findOneById(item) match {
            case _ =>
              ItemServiceImpl.saveUsingDbo(item, MongoDBObject("$pullAll" -> MongoDBObject(Item.Keys.sharedInCollections -> collIds)) ,false)
              true
          }
        } catch {
          case e: SalatDAOUpdateError => false
        }
      })
      if (failedItems.size > 0) {
        Left(InternalError("failed to unshare collections for items: " + failedItems))
      } else {
        Right(items)
      }
    }

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
        "ownerOrgId" -> JsString(c.coll.ownerOrgId),
        "permission" -> JsString(Permission.toHumanReadable(c.access)),
        "itemCount" -> JsNumber(c.coll.itemCount),
        "isPublic" -> JsBoolean(c.coll.isPublic),
        "id" -> JsString(c.coll.id.toString)))
    }
  }
}
