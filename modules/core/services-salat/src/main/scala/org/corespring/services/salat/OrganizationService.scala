package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.WriteConcern
import com.mongodb.{ DBObject, BasicDBList }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.{ Context, grater }
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError
import org.corespring.{ services => interface }
import org.corespring.models.{ ContentCollRef, ContentCollection, MetadataSetRef, Organization }
import org.corespring.models.auth.Permission

import scalaz.{ Failure, Success, Validation }

class OrganizationService(
  val dao: SalatDAO[Organization, ObjectId],
  implicit val context: Context,
  collectionService: => interface.ContentCollectionService,
  metadataSetService: interface.metadata.MetadataSetService,
  itemService: interface.item.ItemService) extends interface.OrganizationService with HasDao[Organization, ObjectId] {

  lazy val logger: Logger = Logger(classOf[OrganizationService])

  object Keys {
    val DEFAULT = "default"
    val collectionId = "collectionId"
    val contentcolls = "contentcolls"
    val id = "_id"
    val metadataId = "metadataId"
    val metadataSets = "metadataSets"
    val name = "name"
    val path = "path"
    val pval = "pval"
  }

  @deprecated("use getDefaultCollection instead", "1.0")
  override def defaultCollection(oid: ObjectId): Option[ObjectId] = {
    getDefaultCollection(oid).toOption.map(_.id)
  }

  @deprecated("use getDefaultCollection instead", "1.0")
  override def defaultCollection(o: Organization): Option[ObjectId] = {
    getDefaultCollection(o.id).toOption.map(_.id)
  }

  override def addMetadataSet(orgId: ObjectId, setId: ObjectId, checkExistence: Boolean): Validation[String, MetadataSetRef] = {

    def metadataSetExists(setId: ObjectId) = metadataSetService.findOneById(setId) != None

    def addMetadataSetRef(setId:ObjectId) = try {
      val ref = MetadataSetRef(setId, true)
      val wr = dao.update(MongoDBObject(Keys.id -> orgId),
        MongoDBObject("$push" -> MongoDBObject(Keys.metadataSets -> grater[MetadataSetRef].asDBObject(ref))),
        false, false)
      if (wr.getLastError.ok()) {
        Success(ref)
      } else {
        Failure("error while updating organization data")
      }
    } catch {
      case e: SalatDAOUpdateError => Failure("error while updating organization data")
    }

    if(checkExistence){
      if(metadataSetExists(setId)) {
        addMetadataSetRef(setId)
      } else {
        Failure("couldn't find the metadata set")
      }
    } else {
      addMetadataSetRef(setId)
    }
  }

  override def updateOrganization(org: Organization): Validation[PlatformServiceError, Organization] = {
    val result = changeName(org.id, org.name)
    logger.debug(s"function=updateOrganization, change name result=$result")
    dao.findOneById(org.id) match {
      case None => Failure(PlatformServiceError(s"org with id: $org.id not found"))
      case Some(o) => Success(o)
    }
  }

  override def changeName(orgId: ObjectId, name: String): Validation[PlatformServiceError, ObjectId] = try {
    logger.debug(s"function=changeName, orgId=$orgId, name=$name")
    val query = MongoDBObject(Keys.id -> orgId)
    val update = MongoDBObject("$set" -> MongoDBObject(Keys.name -> name))
    val result = dao.update(query, update, false, false, dao.collection.writeConcern)
    if (result.getN == 1) Success(orgId) else Failure(PlatformServiceError("Nothing updated"))
  } catch {
    case e: SalatDAOUpdateError => Failure(PlatformServiceError("unable to update organization", e))
    case t: Throwable => Failure(PlatformServiceError("Error updating", t))
  }

  private def toggleCollectionEnabled(orgId: ObjectId, collectionId: ObjectId, enabled: Boolean): Validation[PlatformServiceError, ContentCollRef] = {
    val query = MongoDBObject(Keys.id -> orgId, "contentcolls.collectionId" -> collectionId)
    val update = MongoDBObject("$set" -> MongoDBObject("contentcolls.$.enabled" -> enabled))
    val projection = MongoDBObject(Keys.contentcolls -> MongoDBObject("$elemMatch" -> MongoDBObject(Keys.collectionId -> collectionId)))

    dao.update(query, update)
    dao.collection.find(query, projection).toSeq match {
      case Seq(dbo) => {
        val dboRef: DBObject = dbo.asInstanceOf[DBObject].get(Keys.contentcolls).asInstanceOf[BasicDBList].get(0).asInstanceOf[DBObject]
        val ref: ContentCollRef = com.novus.salat.grater[ContentCollRef].asObject(new MongoDBObject(dboRef))
        Success(ref)
      }
      case _ => Failure(PlatformServiceError("organization not found"))
    }
  }

  /** Enable this collection for this org */
  override def enableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, true)
  }

  /** Enable the collection for the org */
  override def disableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, false)
  }

  override def getOrgsWithAccessTo(collectionId: ObjectId): Stream[Organization] = {
    val query = MongoDBObject("contentcolls.collectionId" -> MongoDBObject("$in" -> List(collectionId)))
    dao.find(query).toStream
  }

  override def isChild(parentId: ObjectId, childId: ObjectId): Boolean = {
    findOneById(childId) match {
      case Some(child) => {
        if (child.path.size >= 2) child.path(1) == parentId else false
      }
      case None => false
    }
  }

  /**
   * remove metadata set by id
   * @param orgId
   * @param metadataId
   * @return maybe an error string
   */
  override def removeMetadataSet(orgId: ObjectId, metadataId: ObjectId): Validation[PlatformServiceError, MetadataSetRef] =
    findOneById(orgId).map {
      org =>
        val query = MongoDBObject(Keys.id -> orgId, "metadataSets.metadataId" -> metadataId)
        logger.trace(s"function=removeMetadataSet, query=${query}")
        val pull = MongoDBObject("$pull" -> MongoDBObject(Keys.metadataSets -> MongoDBObject(Keys.metadataId -> metadataId)))
        val result = dao.update(query, pull, false, false, dao.collection.writeConcern)

        logger.debug(s"function=removeMetadataSet, writeResult.getN=${result.getN}")

        if (result.getLastError.ok) {
          if (result.getN != 1) {
            Failure(PlatformServiceError("Couldn't remove metadata set from org: $orgId, metadataId: $metadataId"))
          } else {
            logger.trace(s"function=removeMetadataSets, org.metadataSets=${org.metadataSets}, setId=$metadataId")
            val ref = org.metadataSets.find(_.metadataId == metadataId)
            logger.trace(s"function=removeMetadataSets, ref=$ref")
            Success(ref.get)
          }
        } else {
          Failure(PlatformServiceError("Error updating orgs"))
        }
    }.getOrElse(Failure(PlatformServiceError(("Can't find org with id: " + orgId))))

  /**
   * TODO: I'm duplicating hasCollRef, but adjusting the query so that it checks that the stored permission pval is gte than the
   * requested pval.
   * Permissions with a higher pval have access to lower pvals, eg: pval 3 can allow pvals 1,2 and 3.
   * see: https://www.pivotaltracker.com/s/projects/880382/stories/63449984
   */
  override def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean = {

    def isRequestForPublicCollection(collectionId: ObjectId, permission: Permission) =
      collectionService.isPublic(collectionId) && permission == Permission.Read

    val query = MongoDBObject(
      Keys.id -> orgId,
      Keys.contentcolls ->
        MongoDBObject(
          "$elemMatch" ->
            MongoDBObject(
              Keys.collectionId -> collectionId,
              Keys.pval -> MongoDBObject("$gte" -> permission.value))))

    val access = isRequestForPublicCollection(collectionId, permission) || dao.count(query) > 0
    logger.trace(s"[canAccessCollection] orgId: $orgId -> $collectionId ? $access")
    access
  }

  override def canAccessCollection(org: Organization, collectionId: ObjectId, permission: Permission): Boolean = canAccessCollection(org.id, collectionId, permission)

  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  override def insert(org: Organization, optParentId: Option[ObjectId]): Validation[PlatformServiceError, Organization] = {

    def update(path: Seq[ObjectId]): Organization = {
      org.copy(
        id = org.id,
        path = Seq(org.id) ++ path,
        contentcolls = org.contentcolls ++ collectionService.getPublicCollections.map(cc => ContentCollRef(cc.id, Permission.Read.value)))
    }

    val paths = {
      for {
        parentId <- optParentId
        parent <- findOneById(parentId)
      } yield {
        parent.path
      }
    }.getOrElse(Seq.empty)

    val updatedOrg = update(paths)

    dao.insert(updatedOrg) match {
      case Some(id) => Success(org)
      case None => Failure(PlatformServiceError("error inserting organization"))
    }
  }

  override def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        addCollectionReference(orgId, collRef) match {
          case Success(_) => Success(collRef)
          case Failure(e) => Failure(e)
        }
      } else {
        Failure(PlatformServiceError("collection reference already exists"))
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  override def addCollectionReference(orgId: ObjectId, reference: ContentCollRef): Validation[PlatformServiceError, Unit] = {
    val query = MongoDBObject(Keys.id -> orgId)
    val update = MongoDBObject("$addToSet" -> MongoDBObject(Keys.contentcolls -> com.novus.salat.grater[ContentCollRef].asDBObject(reference)))
    val result = dao.update(query, update, false, false)
    if (result.getLastError.ok) Success() else Failure(PlatformServiceError(s"Error adding collection reference to org: $orgId, reference: $reference"))
  }

  override def getPermissions(orgId: ObjectId, collId: ObjectId): Permission = {
    getTree(orgId).foldRight[Permission](Permission.None)((o, p) => {
      o.contentcolls.find(_.collectionId == collId) match {
        case Some(ccr) => Permission.fromLong(ccr.pval).getOrElse(p)
        case None => p
      }
    })
  }

  override def getDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection] = {
    val collections = collectionService.getCollectionIds(orgId, Permission.Write, false)
    if (collections.isEmpty) {
      collectionService.insertCollection(orgId, ContentCollection(Keys.DEFAULT, orgId), Permission.Write)
    } else {
      collectionService.getDefaultCollection(collections) //TODO: Move to collectionService
      //findOne(
      //MongoDBObject(Keys.id -> MongoDBObject("$in" -> collections), name -> DEFAULT))
      match {
        case Some(default) => Success(default)
        case None =>
          collectionService.insertCollection(orgId, ContentCollection(Keys.DEFAULT, orgId), Permission.Write)
      }
    }
  }

  override def findOneById(orgId: ObjectId): Option[Organization] = try {
    dao.findOneById(orgId)
  } catch {
    case t: Throwable => {
      t.printStackTrace()
      throw t
    }
  }

  override def findOneByName(name: String): Option[Organization] = dao.findOne(MongoDBObject("name" -> name))

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  override def getTree(parentId: ObjectId): Seq[Organization] = dao.find(MongoDBObject(Keys.path -> parentId)).toSeq

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  override def delete(orgId: ObjectId): Validation[PlatformServiceError, Unit] = {
    try {
      dao.remove(MongoDBObject(Keys.path -> orgId))
      Success(())
    } catch {
      case e: SalatRemoveError => Failure(PlatformServiceError("failed to destroy organization tree", e))
    }
  }

  override def updateCollection(orgId: ObjectId, collRef: ContentCollRef): Validation[PlatformServiceError, ContentCollRef] = {
    if (!hasCollRef(orgId, collRef)) {
      Failure(PlatformServiceError("can't update collection, it does not exist in this organization"))
    } else {
      // pull the old collection
      try {
        dao.update(
          MongoDBObject(Keys.id -> orgId),
          MongoDBObject("$pull" -> MongoDBObject(Keys.contentcolls -> MongoDBObject(Keys.collectionId -> collRef.collectionId))),
          false,
          false,
          dao.collection.writeConcern)
      } catch {
        case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
      }
      // add the updated one
      try {
        dao.update(
          MongoDBObject(Keys.id -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(Keys.contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false,
          false,
          dao.collection.writeConcern)
      } catch {
        case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
      }

      Success(collRef)
    }
  }

  override def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean = {
    dao.findOne(MongoDBObject(Keys.id -> orgId,
      Keys.contentcolls -> MongoDBObject("$elemMatch" ->
        MongoDBObject(Keys.collectionId -> collRef.collectionId, Keys.pval -> collRef.pval)))).isDefined
  }

  override def removeCollection(orgId: ObjectId, collId: ObjectId): Validation[PlatformServiceError, Unit] = {
    findOneById(orgId) match {
      case Some(org) => {
        val updated = org.copy(contentcolls = org.contentcolls.filter(_.collectionId != collId))
        try {
          dao.update(MongoDBObject(Keys.id -> orgId), updated, false, false, dao.collection.writeConcern)
          Success(())
        } catch {
          case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
        }
      }
      case None => Failure(PlatformServiceError("could not find organization"))
    }
  }

  override def deleteCollectionFromAllOrganizations(collId: ObjectId): Validation[String, Unit] = {

    def removeCollectionIdFromOrg() = {
      val query = MongoDBObject(Keys.contentcolls + "." + Keys.collectionId -> collId)
      val update = MongoDBObject("$pull" -> MongoDBObject(Keys.contentcolls -> MongoDBObject(Keys.collectionId -> collId)))
      val result = dao.update(query, update, false, true)
      if (result.getLastError.ok) Success() else Failure(s"remove collectionId $collId from orgs failed")
    }

    def removeCollectionIdFromItem() = {
      itemService.deleteFromSharedCollections(collId).leftMap(e => e.message)
    }

    for {
      rmFromOrg <- removeCollectionIdFromOrg()
      rmFromItem <- removeCollectionIdFromItem()
    } yield Success()
  }

  /**
   * Add the public collection to all orgs to that they have access to it
   * @param collectionId
   * @return
   */
  override def addPublicCollectionToAllOrgs(collectionId: ObjectId): Validation[PlatformServiceError, Unit] = {
    val query = MongoDBObject.empty
    val update = MongoDBObject("$addToSet" -> MongoDBObject(Keys.contentcolls -> collectionId))
    val result = dao.update(query, update, false, true)
    if (result.getLastError.ok) Success() else Failure(PlatformServiceError(s"Error adding public collection to all orgs $collectionId"))
  }

  override def orgsWithPath(orgId: ObjectId, deep: Boolean): Seq[Organization] = {
    val cursor = if (deep) dao.find(MongoDBObject(Keys.path -> orgId)) else dao.find(MongoDBObject(Keys.id -> orgId)) //find the tree of the given organization
    cursor.toSeq
  }

  override def getOrgPermissionForItem(orgId: ObjectId, itemId: VersionedId[ObjectId]): Permission = {
    itemService.collectionIdForItem(itemId).map { collectionId =>
      try {
        getPermissions(orgId, collectionId)
      } catch {
        case t: Throwable => {

          if (logger.isDebugEnabled) {
            t.printStackTrace()
          }

          logger.error(t.getMessage)
          Permission.None
        }
      }
    }.getOrElse {
      logger.warn(s"function=getOrgPermissionsForItem, Can't find item with id=$itemId")
      Permission.None
    }
  }
}
