package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatInsertError, SalatRemoveError }
import grizzled.slf4j.Logger
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionUpdate
import org.corespring.services.errors.PlatformServiceError
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

    //TODO: apply two-phase commit
    try {
      dao.insert(collection) match {
        case Some(_) => try {
          val reference = new ContentCollRef(collection.id, p.value, enabled)
          organizationService.addCollectionReference(orgId, reference).map(_ => collection)
        } catch {
          case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to update organization with collection", e))
        }
        case None => Failure(PlatformServiceError("failed to insert content collection"))
      }
    } catch {
      case e: SalatInsertError => Failure(PlatformServiceError("failed to insert content collection", e))
    }
  }

  /**
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   * TODO: Do we check perms here? or keep it outside of this scope?
   * We'll have to filter the individual item ids anyway
   */
  override def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {

    def allowedToWriteCollection: Validation[PlatformServiceError, Unit] = {
      isAuthorized(orgId, collId, Permission.Write) match {
        case true => Success()
        case _ => Failure(PlatformServiceError("organization does not have write permission on collection"))
      }
    }

    def allowedToReadItems: Validation[PlatformServiceError, Unit] = {
      val objectIds = items.map(i => i.id)
      // get a list of any items that were not authorized to be added
      val notAuthorizedItems = itemService.findMultipleById(objectIds: _*).filterNot(item => {
        println(s"++++++++++++++++ ${item}")
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val sharedInCollections = item.sharedInCollections.map( _ + "")
        println(s"++++++++++++++++ ${sharedInCollections}")
        val collectionsToAuth = item.collectionId +: sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.
          filter(collectionId =>
            isAuthorized(orgId, new ObjectId(collectionId), Permission.Read))
        collectionsAuthorized.nonEmpty
      })
      if(notAuthorizedItems.length > 0){
        logger.error(s"[allowedToReadItems] unable to read items: ${notAuthorizedItems.map(_.id)}")
        Failure(PlatformServiceError("items failed auth"))
      } else {
        Success()
      }
    }

    def saveUnsavedItems: Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
      val savedUnsavedItems = items.partition(item => {
        try {
          itemService.findOneById(item) match {
            case Some(i) if collId.equals(i.collectionId) => true
            case _ => itemService.addCollectionIdToSharedCollections(item, collId).fold(_ => false, _ => true)
          }
        } catch {
          case e: SalatDAOUpdateError => false
        }
      })
      if (savedUnsavedItems._2.nonEmpty) {
        logger.warn(s"[saveUnsavedItems] failed to add items: ${savedUnsavedItems._2.map(_.id).mkString(",")}")
        Failure(PlatformServiceError("failed to add items"))
      } else {
        logger.trace(s"[saveUnsavedItems] added items: ${savedUnsavedItems._1.map(_.id).mkString(",")}")
        Success(savedUnsavedItems._1)
      }
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
    // make sure org has auth for all the collIds
    val authorizedCollIds = collIds.filter(id => isAuthorized(orgId, id, Permission.Write))
    if (authorizedCollIds.size != collIds.size) {
      Failure(PlatformServiceError("authorization failed on collection(s)"))
    } else {
      itemService.removeCollectionIdsFromShared(items, collIds) match {
        case Failure(failedItems) => Failure(PlatformServiceError("failed to unshare collections for items: " + failedItems))
        case Success(_) => Success(items)
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
        Failure(PlatformServiceError("failed to unshare collections for items: " + failedItems))
      } else {
        Success(items)
      }*/
    //}

  }

  /**
   *
   * @return
   * def addOrganizations(orgs: Seq[(ObjectId, Permission)], collId: ObjectId): Validation[PlatformServiceError, Unit] = {
   * val errors = orgs.map(org => organizationService.addCollection(org._1, collId, org._2)).filter(_.isLeft)
   * if (errors.size > 0) Failure(errors(0).left.get)
   * else Success(())
   * }
   */

  override def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId] = getContentCollRefs(orgId, p, deep).map(_.collectionId)

  override def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef] = {

    val orgs = organizationService.orgsWithPath(orgId, deep)

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
  override def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean = {
    val orgCollectionIds = getCollectionIds(orgId, p)
    val hasId = orgCollectionIds.contains(collId)
    if (!hasId) {
      logger.debug(s"[isAuthorized] == false : orgId: $orgId, collection id: $collId isn't in: ${orgCollectionIds.mkString(",")}")
    }
    hasId
  }

  override def delete(collId: ObjectId): Validation[PlatformServiceError, Unit] = {
    //todo: roll backs after detecting error in organization update
    try {
      val collectionItemCount = itemCount(collId)
      if (collectionItemCount != 0) {
        Failure(PlatformServiceError(s"Can't delete this collection it has $collectionItemCount item(s) in it."))
      } else {
        dao.removeById(collId)
        organizationService.deleteCollectionFromAllOrganizations(collId)
        itemService.deleteFromSharedCollections(collId)
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to transfer collection to archive", e))
      case e: SalatRemoveError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  override def getPublicCollections: Seq[ContentCollection] = dao.find(MongoDBObject(Keys.isPublic -> true)).toSeq

  override def update(id: ObjectId, update: ContentCollectionUpdate): Validation[PlatformServiceError, ContentCollection] = {
    try {

      val query = MongoDBObject("_id" -> id)

      val updateDbo = {
        val builder = MongoDBObject.newBuilder
        update.isPublic.map(p => builder += "isPublic" -> p)
        update.name.map(n => builder += "name" -> n)
        builder.result()
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

  /*
   * Share the items returned by the query with the specified collection.
   *
   * @param orgId
   * @param query
   * @param collId
   * @return
   *
   *          "add filtered items to a collection" in new CollectionSharingScope {
   *
   * publishItemsInCollection(collectionB1)
   * // this is to support a user searching for a set of items, then adding that set of items to a collection
   * // share items in collection b1 that are published with collection a1...
   * val query = s""" {"published":true, "collectionId":{"$$in":["$collectionB1"]} } """
   * val addFilteredItemsReq = FakeRequest("", s"?q=$query&access_token=$accessTokenA")
   * val shareItemsResult = CollectionApi.shareFilteredItemsWithCollection(collectionA1, Some(query))(addFilteredItemsReq)
   * assertResult(shareItemsResult)
   * val response = parsed[JsNumber](shareItemsResult)
   * response.toString mustEqual "3"
   * // check how many items are now available in a1. There should be 6: 3 owned by a1 and 3 shared with a1 from b1
   * val listReq = FakeRequest(GET, s"/api/v1/collections/$collectionA1/items?access_token=%s".format(accessTokenA))
   * val listResult = ItemApi.listWithColl(collectionA1, None, None, "10", 0, 10, None)(listReq)
   * assertResult(listResult)
   * val itemsList = parsed[List[JsValue]](listResult)
   * itemsList.size must beEqualTo(6)
   * }
   */

  override def shareItemsMatchingQuery(orgId: ObjectId, query: String, collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    //TODO: RF: implement - need to decide how to abstract search
    Failure(PlatformServiceError("shareItemsMatchingQuery is not supported"))
    //Original --
    /*val acessibleCollections = ContentCollection.getCollectionIds(orgId, Permission.Read)
    val collectionsQuery: DBObject = ItemServiceWired.createDefaultCollectionsQuery(acessibleCollections, orgId)
    val parsedQuery: Validation[SearchCancelled, DBObject] = ItemSearch.toSearchObj(query, Some(collectionsQuery))

    parsedQuery match {
      case Success(searchQry) =>
        val cursor = ItemServiceWired.find(searchQry, MongoDBObject("_id" -> 1))

        val seq = cursor.toSeq
        if (seq.size == 0) {
          logger.warn(s"[shareItemsMatchingQuery] didn't find any items: ${cursor.size}: query: ${JSON.serialize(searchQry)}")
        }
        val ids = seq.map(item => item.id)
        shareItems(orgId, ids, collId)
      case Failure(sc) => sc.error match {
        case None => Success(Seq())
        case Some(error) => Failure(CorespringInternalError(error.clientOutput.getOrElse("error processing search")))
      }
    }*/
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

  private def todo = Failure(PlatformServiceError("todo"))

  override def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Boolean] = todo

  override def listCollectionsByOrg(orgId: ObjectId): Stream[ContentCollection] = {
    val refs = getContentCollRefs(orgId, Permission.Read, true).map(_.collectionId)
    val query = ("_id" $in refs)
    dao.find(query).toStream
  }

  override def shareCollectionWithOrg(collectionId: ObjectId, orgId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef] = todo

  override def create(name: String, org: Organization): Validation[PlatformServiceError, ContentCollection] = {
    val collection = ContentCollection(name = name, ownerOrgId = org.id)
    insertCollection(org.id, collection, Permission.Write, true)
  }
}
