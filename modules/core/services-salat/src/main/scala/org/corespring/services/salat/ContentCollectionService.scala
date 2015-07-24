package org.corespring.services.salat

import com.mongodb.casbah.Imports
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatInsertError, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.services.salat.bootstrap.AppMode
import org.corespring.{ services => interface }
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scalaz.{ Failure, Validation }

class ContentCollectionService(
  val dao: SalatDAO[ContentCollection, ObjectId],
  val context: Context,
  organizationService: => interface.OrganizationService,
  val itemService: interface.item.ItemService,
  val appMode: AppMode) extends interface.ContentCollectionService with HasDao[ContentCollection, ObjectId] {

  def isProd = appMode.isProd

  object Keys {
    val isPublic = "isPublic"
    val sharedInCollections = "sharedInCollections"
  }

  private val logger: Logger = Logger(classOf[ContentCollectionService])

  override def insertCollection(orgId: ObjectId, coll: ContentCollection, p: Permission, enabled: Boolean): Either[PlatformServiceError, ContentCollection] = {

    //TODO: apply two-phase commit
    try {
      val collection = coll.copy(id = if (isProd) ObjectId.get else coll.id)
      dao.insert(collection) match {
        case Some(_) => try {
          val reference = new ContentCollRef(collection.id, p.value, enabled)
          organizationService.addCollectionReference(orgId, reference) match {
            case Left(e) => Left(e)
            case Right(_) => Right(coll)
          }
        } catch {
          case e: SalatDAOUpdateError => Left(PlatformServiceError("failed to update organization with collection", e))
        }
        case None => Left(PlatformServiceError("failed to insert content collection"))
      }
    } catch {
      case e: SalatInsertError => Left(PlatformServiceError("failed to insert content collection", e))
    }
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
  override def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Either[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    if (isAuthorized(orgId, collId, Permission.Write)) {

      if (items.isEmpty) {
        logger.warn("[shareItems] items is empty")
      }

      val objectIds = items.map(i => i.id)

      // get a list of any items that were not authorized to be added
      val itemsNotAuthorized = itemService.findMultipleById(objectIds: _*).filterNot(item => {
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val collectionsToAuth = item.collectionId +: item.sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.filter(collectionId => isAuthorized(orgId, new ObjectId(collectionId), Permission.Read))
        collectionsAuthorized.size > 0
      })
      if (itemsNotAuthorized.size <= 0) {
        // add collection id to item.sharedinCollections unless the collection is the owner collection for item
        val savedUnsavedItems = items.partition(item => {
          try {
            itemService.findOneById(item) match {
              case Some(itemObj) if (collId.equals(itemObj)) => true
              case _ => itemService.addCollectionIdToSharedCollections(item, collId).fold(_ => false, _ => true)
            }
          } catch {
            case e: SalatDAOUpdateError => false
          }
        })
        if (savedUnsavedItems._2.size > 0) {
          logger.warn(s"[addItems] failed to add items: ${savedUnsavedItems._2.map(_.id).mkString(",")}")
          Left(PlatformServiceError("failed to add items"))
        } else {
          logger.trace(s"[addItems] added items: ${savedUnsavedItems._1.map(_.id).mkString(",")}")
          Right(savedUnsavedItems._1)
        }

      } else {
        Left(PlatformServiceError("items failed auth: " + itemsNotAuthorized.map(_.id)))
      }
    } else {
      Left(PlatformServiceError("organization does not have write permission on collection"))
    }
  }

  /** Get a default collection from the set of ids */
  override def getDefaultCollection(collections: Seq[ObjectId]): Option[ContentCollection] = dao.findOne(MongoDBObject("_id" -> MongoDBObject("$in" -> collections), "name" -> "default"))

  /**
   * Unshare the specified items from the specified collections
   *
   * @param orgId
   * @param items - sequence of items to be unshared from
   * @param collIds - sequence of collections to have the items removed from
   * @return
   */
  override def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Either[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    // make sure org has auth for all the collIds
    val authorizedCollIds = collIds.filter(id => isAuthorized(orgId, id, Permission.Write))
    if (authorizedCollIds.size != collIds.size) {
      Left(PlatformServiceError("authorization failed on collection(s)"))
    } else {
      itemService.removeCollectionIdsFromShared(items, collIds) match {
        case Left(failedItems) => Left(PlatformServiceError("failed to unshare collections for items: " + failedItems))
        case Right(_) => Right(items)
      }
      /*val failedItems = items.filterNot(item => {
        try {
          itemService.findOneById(item) match {
            case _ =>
              itemService.removeCollectionIdsFromShared(collId)
              //saveUsingDbo(item, MongoDBObject("$pullAll" -> MongoDBObject(Item.Keys.sharedInCollections -> collIds)), false)
              true
          }
        } catch {
          case e: SalatDAOUpdateError => false
        }*/
    } //)
    /*if (failedItems.size > 0) {
        Left(PlatformServiceError("failed to unshare collections for items: " + failedItems))
      } else {
        Right(items)
      }*/
    //}

  }

  /**
   *
   * @param orgs contains a sequence of (organization id -> permission) tuples
   * @param collId
   * @return
   */
  def addOrganizations(orgs: Seq[(ObjectId, Permission)], collId: ObjectId): Either[PlatformServiceError, Unit] = {
    val errors = orgs.map(org => organizationService.addCollection(org._1, collId, org._2)).filter(_.isLeft)
    if (errors.size > 0) Left(errors(0).left.get)
    else Right(())
  }

  override def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId] = getContentCollRefs(orgId, p, deep).map(_.collectionId)

  override def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef] = {

    val orgs = organizationService.orgsWithPath(orgId, deep)

    def addRefsWithPermission(org: Organization, acc: Seq[ContentCollRef]): Seq[ContentCollRef] = {
      acc ++ org.contentcolls.filter(ref => (ref.pval & p.value) == p.value)
    }

    val out = orgs.foldRight[Seq[ContentCollRef]](Seq.empty)(addRefsWithPermission)

    if (p == Permission.Read) {
      out ++ getPublicCollections.map(c => ContentCollRef(c.id, Permission.Read.value, true))
    } else {
      out
    }
  }

  override def getCollections(orgId: ObjectId, p: Permission): Either[PlatformServiceError, Seq[ContentCollection]] = {
    val collectionIds = getCollectionIds(orgId, p, false)
    Right(dao.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collectionIds))).toSeq)
  }

  def isPublic(collectionId: ObjectId): Boolean = dao.findOneById(collectionId).exists(_.isPublic)

  /**
   * does the given organization have access to the given collection with given permissions?
   * @param orgId
   * @param collId
   */
  override def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean = {
    val orgCollectionIds = getCollectionIds(orgId, p)
    val exists = orgCollectionIds.exists(_ == collId)
    if (!exists) {
      logger.debug(s"[isAuthorized] == false : orgId: $orgId, collection id: $collId isn't in: ${orgCollectionIds.mkString(",")}")
    }
    exists
  }

  override def delete(collId: ObjectId): Validation[PlatformServiceError, Unit] = {
    //todo: roll backs after detecting error in organization update
    try {

      //TODO: RF: Once we move the logic to the appropriate service, run this stuff in a for{ .. }
      //TODO: RF: Move to services
      dao.removeById(collId)
      organizationService.deleteCollectionFromAllOrganizations(collId)
      import scalaz._
      Validation.fromEither(itemService.deleteFromSharedCollections(collId))
      /*Organization.find(
        MongoDBObject(
          Organization.contentcolls + "." + ContentCollRef.collectionId -> collId))
        .foldRight[Validation[PlatformServiceError, Unit]](Success(()))((org, result) => {
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
            case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
          }
        } else result
      })
      */
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to transfer collection to archive", e))
      case e: SalatRemoveError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  override def getPublicCollections: Seq[ContentCollection] = dao.find(MongoDBObject(Keys.isPublic -> true)).toSeq

  override def updateCollection(coll: ContentCollection): Either[PlatformServiceError, ContentCollection] = {
    try {
      dao.update(MongoDBObject("_id" -> coll.id), coll, false, false, dao.collection.writeConcern)
      if (coll.isPublic) {
        organizationService.addPublicCollectionToAllOrgs(coll.id)
      }
      dao.findOneById(coll.id) match {
        case Some(coll) => Right(coll)
        case None => Left(PlatformServiceError("could not find the collection that was just updated"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(PlatformServiceError("failed to update collection", e))
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
  override def shareItemsMatchingQuery(orgId: ObjectId, query: String, collId: ObjectId): Either[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    //TODO: RF: implement - need to decide how to abstract search
    Right(Seq.empty)
    //Original --
    /*val acessibleCollections = ContentCollection.getCollectionIds(orgId, Permission.Read)
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
    }*/
  }

  /** How many items are associated with this collectionId */
  override def itemCount(collectionId: ObjectId): Long = dao.count(MongoDBObject("collectionId" -> collectionId.toString))

  override def findByDbo(dbo: Imports.DBObject): Stream[ContentCollection] = ???

  override def findOneById(id: Imports.ObjectId): Option[ContentCollection] = ???
}
