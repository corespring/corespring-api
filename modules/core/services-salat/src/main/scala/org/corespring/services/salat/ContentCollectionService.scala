package org.corespring.services.salat

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatInsertError, SalatRemoveError }
import grizzled.slf4j.Logger
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionUpdate
import org.corespring.services.errors._
import org.corespring.{ services => interface }

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

class ContentCollectionService(
  val dao: SalatDAO[ContentCollection, ObjectId],
  val context: Context,
  organizationService: => interface.OrganizationService,
  val itemService: interface.item.ItemService,
  archiveConfig: ArchiveConfig) extends interface.ContentCollectionService with HasDao[ContentCollection, ObjectId] {

  object Keys {
    val isPublic = "isPublic"
    val sharedInCollections = "sharedInCollections"
  }

  private val logger: Logger = Logger(classOf[ContentCollectionService])

  /** Enable this collection for this org */
  override def enableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    organizationService.enableCollection(orgId, collectionId)
  }

  /** Enable the collection for the org */
  override def disableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    organizationService.disableCollection(orgId, collectionId)
  }

  override def insertCollection(orgId: ObjectId, collection: ContentCollection, p: Permission, enabled: Boolean): Validation[PlatformServiceError, ContentCollection] = {

    def addCollectionToDb() = {
      try {
        dao.insert(collection) match {
          case Some(_) => Success(collection)
          case None => Failure(CollectionInsertError(collection, None))
        }
      } catch {
        case e: SalatInsertError =>
          Failure(CollectionInsertError(collection, Some(e)))
      }
    }

    def addCollectionToOrg(collection: ContentCollection) = {
      try {
        val reference = new ContentCollRef(collection.id, p.value, enabled)
        organizationService.addCollectionReference(orgId, reference) match {
          case Success(_) => Success(collection)
          case _ => Failure(OrganizationAddCollectionError(orgId, collection.id, p, None))
        }
      } catch {
        case e: SalatDAOUpdateError => {
          Failure(OrganizationAddCollectionError(orgId, collection.id, p, Some(e)))
        }
      }
    }

    //TODO: apply two-phase commit
    for {
      addedCollection <- addCollectionToDb()
      updatedCollection <- addCollectionToOrg(addedCollection)
    } yield updatedCollection
  }

  /**
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   * TODO: Do we check perms here? or keep it outside of this scope?
   * We'll have to filter the individual item ids anyway
   */
  override def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {

    def allowedToWriteCollection = {
      isAuthorized(orgId, collId, Permission.Write)
    }

    def allowedToReadItems = {
      val objectIds = items.map(i => i.id)
      // get a list of any items that were not authorized to be added
      itemService.findMultipleById(objectIds: _*).filterNot(item => {
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val collectionsToAuth = new ObjectId(item.collectionId) +: item.sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.
          filter(collectionId => isAuthorized(orgId, collectionId, Permission.Read).isSuccess)
        collectionsAuthorized.nonEmpty
      }) match {
        case Stream.Empty => Success()
        case notAuthorizedItems => {
          logger.error(s"[allowedToReadItems] unable to read items: ${notAuthorizedItems.map(_.id)}")
          Failure(ItemAuthorizationError(orgId, Permission.Read, notAuthorizedItems.map(_.id): _*))
        }
      }
    }

    def saveUnsavedItems = {
      itemService.addCollectionIdToSharedCollections(items, collId)
    }

    for {
      canWriteToCollection <- allowedToWriteCollection
      canReadAllItems <- allowedToReadItems
      sharedItems <- saveUnsavedItems
    } yield sharedItems
  }

  /** Get a default collection from the set of ids */
  override def getDefaultCollection(collections: Seq[ObjectId]): Option[ContentCollection] =
    dao.findOne(MongoDBObject("_id" -> MongoDBObject("$in" -> collections), "name" -> "default"))

  /**
   * Unshare the specified items from the specified collections
   */
  override def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    for {
      canUpdateAllCollections <- isAuthorized(orgId, collIds, Permission.Write)
      successfullyRemovedItems <- itemService.removeCollectionIdsFromShared(items, collIds)
    } yield successfullyRemovedItems
  }

  override def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId] = getContentCollRefs(orgId, p, deep).map(_.collectionId)

  override def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef] = {

    val orgs = organizationService.orgsWithPath(orgId, deep)

    logger.trace(s"function=getContentCollRefs, orgId=$orgId, orgs=$orgs")
    def addRefsWithPermission(org: Organization, acc: Seq[ContentCollRef]): Seq[ContentCollRef] = {
      acc ++ org.contentcolls.filter(ref => (ref.pval & p.value) == p.value)
    }

    val out = orgs.foldRight[Seq[ContentCollRef]](Seq.empty)(addRefsWithPermission)

    if (p == Permission.Read) {
      out ++ getPublicCollections.map(c => ContentCollRef(c.id, Permission.Read.value, enabled = true))
    } else {
      out
    }
  }

  override def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]] = {
    val collectionIds = getCollectionIds(orgId, p, deep = false)
    Success(dao.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collectionIds))).toSeq)
  }

  def isPublic(collectionId: ObjectId): Boolean = dao.findOneById(collectionId).exists(_.isPublic)

  /**
   * does the given organization have access to the given collection with given permissions?
   */
  override def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, Unit] = {
    isAuthorized(orgId, Seq(collId), p)
  }

  /**
   * does the given organization have access to all the given collections with given permissions?
   */
  override def isAuthorized(orgId: ObjectId, collIds: Seq[ObjectId], p: Permission): Validation[PlatformServiceError, Unit] = {
    val orgCollectionIds = getCollectionIds(orgId, p)

    collIds.filterNot(id => orgCollectionIds.contains(id)) match {
      case Nil => Success()
      case failedCollIds => Failure(CollectionAuthorizationError(orgId, p, failedCollIds: _*))
    }
  }

  override def delete(collId: ObjectId): Validation[PlatformServiceError, Unit] = {
    //todo: roll backs after detecting error in organization update

    def isEmptyCollection = itemCount(collId) match {
      case 0 => Success()
      case n => Failure(PlatformServiceError(s"Can't delete this collection it has $n item(s) in it."))
    }

    def doDelete() = {
      try {
        dao.removeById(collId)
        organizationService.deleteCollectionFromAllOrganizations(collId)
        itemService.deleteFromSharedCollections(collId)
        Success()
      } catch {
        case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to transfer collection to archive", e))
        case e: SalatRemoveError => Failure(PlatformServiceError(e.getMessage))
      }
    }

    for {
      canBeDeleted <- isEmptyCollection
      success <- doDelete
    } yield ()
  }

  override def getPublicCollections: Seq[ContentCollection] = dao.find(MongoDBObject(Keys.isPublic -> true)).toSeq

  override def update(id: ObjectId, update: ContentCollectionUpdate): Validation[PlatformServiceError, ContentCollection] = {
    try {

      val query = MongoDBObject("_id" -> id)

      val updateDbo = {
        val fields = Seq(
          update.isPublic.map(p => "isPublic" -> p),
          update.name.map(n => "name" -> n)).flatten
        $set(fields: _*)
      }

      val result = dao.update(query, updateDbo, upsert = false, multi = false, dao.collection.writeConcern)

      if (result.getN == 1) {
        if (update.isPublic.exists(_ == true)) {
          organizationService.addPublicCollectionToAllOrgs(id)
        }
        dao.findOneById(id).toSuccess(PlatformServiceError(s"Can't find collection with id: $id"))
      } else {
        Failure(PlatformServiceError(s"No update occurred for query: $id"))
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to update collection", e))
    }
  }

  /** How many items are associated with this collectionId */
  override def itemCount(collectionId: ObjectId): Long = {
    itemService.count(MongoDBObject("collectionId" -> collectionId.toString))
  }

  override def findOneById(id: ObjectId): Option[ContentCollection] = dao.findOneById(id)

  override def archiveCollectionId: ObjectId = archiveConfig.contentCollectionId

  override def count(dbo: DBObject): Long = dao.count(dbo)

  override def findByDbo(dbo: DBObject, fields: Option[DBObject] = None, sort: Option[DBObject], skip: Int, limit: Int): Stream[ContentCollection] = {
    dao.find(dbo, fields.getOrElse(MongoDBObject.empty)).sort(sort.getOrElse(MongoDBObject.empty)).skip(skip).limit(limit).toStream
  }

  def getCollection(collectionId: ObjectId): Validation[PlatformServiceError, ContentCollection] = {
    findOneById(collectionId) match {
      case Some(collection) => Success(collection)
      case _ => Failure(PlatformServiceError(s"Collection not found: $collectionId"))
    }
  }

  override def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Unit] = {
    for {
      collection <- getCollection(collectionId)
      result <- if (collection.ownerOrgId == org.id) Success() else Failure(PlatformServiceError(s"Organisation ${org.name} does not own collection: $collectionId."))
    } yield result
  }

  override def shareCollectionWithOrg(collectionId: ObjectId, orgId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef] = {
    organizationService.addCollection(orgId, collectionId, p)
  }

  override def listCollectionsByOrg(orgId: ObjectId): Stream[ContentCollection] = {
    val refs = getContentCollRefs(orgId, Permission.Read, true).map(_.collectionId)
    val query = ("_id" $in refs)
    logger.trace(s"function=listCollectionsByOrg, orgId=$orgId, query=$query")
    dao.find(query).toStream
  }

  override def listAllCollectionsAvailableForOrg(orgId: Imports.ObjectId): Stream[CollectionInfo] = {

    logger.trace(s"function=listAllCollectionsAvailableForOrg, orgId=$orgId")
    val refs = getContentCollRefs(orgId, Permission.Read)
    listCollectionsByOrg(orgId)
      .filterNot(_.id == archiveCollectionId)
      .flatMap { c =>
        val permission = refs.find(r => r.collectionId == c.id).flatMap(r => Permission.fromLong(r.pval))

        permission.map(p =>
          CollectionInfo(c, itemCount(c.id), orgId, p))
      }
  }

  override def create(name: String, org: Organization): Validation[PlatformServiceError, ContentCollection] = {
    val collection = ContentCollection(name = name, ownerOrgId = org.id)
    insertCollection(org.id, collection, Permission.Write, true)
  }

  //new api, not used yet
  def shareItemWithCollection(org: ObjectId, item: VersionedId[ObjectId], collection: ObjectId): Validation[PlatformServiceError, VersionedId[ObjectId]] = {
    Failure(PlatformServiceError("Not implemented"))
  }

  override def isItemSharedWith(itemId: VersionedId[ObjectId], collId: ObjectId): Boolean = {
    itemService.findOneById(itemId).map(_.sharedInCollections.contains(collId)).getOrElse(false)
  }
}
