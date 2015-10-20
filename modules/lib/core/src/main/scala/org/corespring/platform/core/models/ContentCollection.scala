package org.corespring.platform.core.models

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.dao._
import org.bson.types.ObjectId
import org.corespring.common.log.ClassLogging
import org.corespring.models.auth.Permission
import org.corespring.models.error.CorespringInternalError
import org.corespring.models.item.Item
import org.corespring.models.search.SearchCancelled
import org.corespring.models.search.{ ItemSearch, Searchable }
import org.corespring.platform.core.services.ContentCollectionService
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import scala.Left
import scala.Right
import scala.Some
import scalaz.Failure
import scalaz.Success
import scalaz.Validation
import se.radley.plugin.salat._
import com.mongodb.util.JSON

/**
 * A ContentCollection
 * ownerOrgId is not a one-to-many with organization. Collections may be in multiple orgs, but they have one 'owner'
 *
 */
case class ContentCollection(
  var name: String = "",
  var ownerOrgId: ObjectId,
  var isPublic: Boolean = false,
  var id: ObjectId = new ObjectId()) {
  lazy val itemCount: Int = ItemServiceWired.find(MongoDBObject("collectionId" -> id.toString)).count
}

object ContentCollection extends ContentCollectionImpl

trait ContentCollectionImpl
  extends ModelCompanion[ContentCollection, ObjectId]
  with Searchable
  with ContentCollectionService
  with ClassLogging {

  val name = "name"
  val isPublic = "isPublic"
  val ownerOrgId = "ownerOrgId"
  val DEFAULT = "default" //used as the value for name when the content collection is a default collection

  val collection = mongoCollection("contentcolls")

  collection.distinct()
  import org.corespring.models.mongoContext.context

  val dao = new SalatDAO[ContentCollection, ObjectId](collection = collection) {}

  def insertCollection(orgId: ObjectId, coll: ContentCollection, p: Permission, enabled: Boolean = true): Either[CorespringInternalError, ContentCollection] = {
    //TODO: apply two-phase commit
    if (Play.isProd) coll.id = new ObjectId()
    try {
      super.insert(coll) match {
        case Some(_) => try {
          Organization.update(MongoDBObject("_id" -> orgId),
            MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> grater[ContentCollRef].asDBObject(new ContentCollRef(coll.id, p.value, enabled)))),
            false, false, Organization.collection.writeConcern)
          Right(coll)
        } catch {
          case e: SalatDAOUpdateError => Left(CorespringInternalError("failed to update organization with collection", e))
        }
        case None => Left(CorespringInternalError("failed to insert content collection"))
      }
    } catch {
      case e: SalatInsertError => Left(CorespringInternalError("failed to insert content collection", e))
    }
  }

  //TODO if public content collection, use two-phase commit and add possibility for rollback
  def updateCollection(coll: ContentCollection): Either[CorespringInternalError, ContentCollection] = {
    try {
      ContentCollection.update(MongoDBObject("_id" -> coll.id), coll, false, false, ContentCollection.collection.writeConcern)
      if (coll.isPublic) {
        Organization.update(MongoDBObject(),
          MongoDBObject("$addToSet" -> MongoDBObject(Organization.contentcolls -> coll.id)),
          false, false, Organization.defaultWriteConcern)
      }
      ContentCollection.findOneById(coll.id) match {
        case Some(coll) => Right(coll)
        case None => Left(CorespringInternalError("could not find the collection that was just updated"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(CorespringInternalError("failed to update collection", e))
    }
  }

  lazy val archiveCollId: ObjectId = {
    val id = new ObjectId("500ecfc1036471f538f24bdc")
    val archiveOrg = new ObjectId("52e68c0bd455283f1744a721");
    ContentCollection.insert(ContentCollection("archiveColl", id = id, ownerOrgId = archiveOrg))
    id
  }

  def delete(collId: ObjectId): Validation[CorespringInternalError, Unit] = {
    //todo: roll backs after detecting error in organization update
    try {
      ContentCollection.removeById(collId)
      Organization.find(
        MongoDBObject(
          Organization.contentcolls + "." + ContentCollRef.collectionId -> collId))
        .foldRight[Validation[CorespringInternalError, Unit]](Success(()))((org, result) => {
          if (result.isSuccess) {
            org.contentcolls = org.contentcolls.filter(_.collectionId != collId)
            try {
              Organization.update(MongoDBObject("_id" -> org.id), org, false, false, Organization.defaultWriteConcern)
              val query = MongoDBObject("sharedInCollections" -> MongoDBObject("$in" -> List(collId)))
              ItemServiceWired.find(query).foreach(item => {
                ItemServiceWired.saveUsingDbo(item.id, MongoDBObject("$pull" -> MongoDBObject(Item.Keys.sharedInCollections -> collId)))
              })
              Success(())
            } catch {
              case e: SalatDAOUpdateError => Failure(CorespringInternalError(e.getMessage))
            }
          } else result
        })

    } catch {
      case e: SalatDAOUpdateError => Failure(CorespringInternalError("failed to transfer collection to archive", e))
      case e: SalatRemoveError => Failure(CorespringInternalError(e.getMessage))
    }
  }

  def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId] = getContentCollRefs(orgId, p, deep).map(_.collectionId)

  def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef] = {
    val cursor = if (deep) Organization.find(MongoDBObject(Organization.path -> orgId)) else Organization.find(MongoDBObject("_id" -> orgId)) //find the tree of the given organization

    def addRefsWithPermission(org: Organization, acc: Seq[ContentCollRef]): Seq[ContentCollRef] = {
      acc ++ org.contentcolls.filter(ref => (ref.pval & p.value) == p.value)
    }

    val out = cursor.foldRight[Seq[ContentCollRef]](Seq.empty)(addRefsWithPermission)

    cursor.close()

    if (p == Permission.Read) {
      out ++ getPublicCollections.map(c => ContentCollRef(c.id, Permission.Read.value, true))
    } else {
      out
    }
  }

  override def getCollections(orgId: ObjectId, p: Permission): Either[CorespringInternalError, Seq[ContentCollection]] = {
    val collectionIds = ContentCollection.getCollectionIds(orgId, p, false);
    Right(ContentCollection.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collectionIds))).toSeq)
  }

  def getPublicCollections: Seq[ContentCollection] = ContentCollection.find(MongoDBObject(isPublic -> true)).toSeq

  def isPublic(collectionId: ObjectId): Boolean = findOneById(collectionId).exists(_.isPublic)

  /**
   *
   * @param orgs contains a sequence of (organization id -> permission) tuples
   * @param collId
   * @return
   */
  def addOrganizations(orgs: Seq[(ObjectId, Permission)], collId: ObjectId): Either[CorespringInternalError, Unit] = {
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
  def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Either[CorespringInternalError, Seq[VersionedId[ObjectId]]] = {
    if (isAuthorized(orgId, collId, Permission.Write)) {

      if (items.isEmpty) {
        logger.warn("[shareItems] items is empty")
      }

      val oids = items.map(i => i.id)
      val query = MongoDBObject("_id._id" -> MongoDBObject("$in" -> oids))

      // get a list of any items that were not authorized to be added
      val itemsNotAuthorized = ItemServiceWired.find(query).filterNot(item => {
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val collectionsToAuth = item.collectionId.map(Seq(_)).getOrElse(Seq.empty) ++ item.sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.filter(collectionId => isAuthorized(orgId, new ObjectId(collectionId), Permission.Read))
        collectionsAuthorized.size > 0
      })
      if (itemsNotAuthorized.size <= 0) {
        // add collection id to item.sharedinCollections unless the collection is the owner collection for item
        val savedUnsavedItems = items.partition(item => {
          try {
            ItemServiceWired.findOneById(item) match {
              case Some(itemObj) if (collId.equals(itemObj)) => true
              case _ =>
                ItemServiceWired.saveUsingDbo(item, MongoDBObject("$addToSet" -> MongoDBObject(Item.Keys.sharedInCollections -> collId)), false)
                true
            }
          } catch {
            case e: SalatDAOUpdateError => false
          }
        })
        if (savedUnsavedItems._2.size > 0) {
          logger.warn(s"[addItems] failed to add items: ${savedUnsavedItems._2.map(_.id).mkString(",")}")
          Left(CorespringInternalError("failed to add items"))
        } else {
          logger.trace(s"[addItems] added items: ${savedUnsavedItems._1.map(_.id).mkString(",")}")
          Right(savedUnsavedItems._1)
        }

      } else {
        Left(CorespringInternalError("items failed auth: " + itemsNotAuthorized.map(_.id)))
      }
    } else {
      Left(CorespringInternalError("organization does not have write permission on collection"))
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
  def shareItemsMatchingQuery(orgId: ObjectId, query: String, collId: ObjectId): Either[CorespringInternalError, Seq[VersionedId[ObjectId]]] = {

    val acessibleCollections = ContentCollection.getCollectionIds(orgId, Permission.Read)
    val collectionsQuery: DBObject = ItemServiceWired.createDefaultCollectionsQuery(acessibleCollections, orgId)
    val parsedQuery: Either[SearchCancelled, DBObject] = ItemSearch.toSearchObj(query, Some(collectionsQuery))

    parsedQuery match {
      case Right(searchQry) =>
        val cursor = ItemServiceWired.find(searchQry, MongoDBObject("_id" -> 1))

        val seq = cursor.toSeq
        if (seq.size == 0) {
          logger.warn(s"[shareItemsMatchingQuery] didn't find any items: ${cursor.size}: query: ${JSON.serialize(searchQry)}")
        }
        val ids = seq.map(item => item.id)
        shareItems(orgId, ids, collId)
      case Left(sc) => sc.error match {
        case None => Right(Seq())
        case Some(error) => Left(CorespringInternalError(error.clientOutput.getOrElse("error processing search")))
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
  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Either[CorespringInternalError, Seq[VersionedId[ObjectId]]] = {
    // make sure org has auth for all the collIds
    val authorizedCollIds = collIds.filter(id => isAuthorized(orgId, id, Permission.Write))
    if (authorizedCollIds.size != collIds.size) {
      Left(CorespringInternalError("authorization failed on collection(s)"))
    } else {
      val failedItems = items.filterNot(item => {
        try {
          ItemServiceWired.findOneById(item) match {
            case _ =>
              ItemServiceWired.saveUsingDbo(item, MongoDBObject("$pullAll" -> MongoDBObject(Item.Keys.sharedInCollections -> collIds)), false)
              true
          }
        } catch {
          case e: SalatDAOUpdateError => false
        }
      })
      if (failedItems.size > 0) {
        Left(CorespringInternalError("failed to unshare collections for items: " + failedItems))
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
        "ownerOrgId" -> JsString(c.coll.ownerOrgId.toString),
        "permission" -> JsString(Permission.toHumanReadable(c.access)),
        "itemCount" -> JsNumber(c.coll.itemCount),
        "isPublic" -> JsBoolean(c.coll.isPublic),
        "id" -> JsString(c.coll.id.toString)))
    }
  }

}
