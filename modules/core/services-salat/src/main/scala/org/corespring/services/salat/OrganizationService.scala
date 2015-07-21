package org.corespring.services.salat

import com.mongodb.{ DBObject, BasicDBList }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.grater
import com.novus.salat.dao.{ SalatDAOUpdateError, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.services.errors.PlatformServiceError
import org.corespring.{ services => interface }
import org.corespring.models.{ ContentCollRef, ContentCollection, MetadataSetRef, Organization }
import org.corespring.models.auth.Permission

import scalaz.{ Success, Validation }

trait OrganizationService extends interface.OrganizationService with HasDao[Organization, ObjectId] {

  lazy val logger: Logger = Logger(classOf[OrganizationService])

  object Keys {
    val name = "name"
    val path = "path"
    val contentcolls = "contentcolls"
    val id = "id"
    val metadataSets = "metadataSets"
    val collectionId = "collectionId"
    val pval = "pval"
    val DEFAULT = "default"
  }

  import Keys._

  def collectionService: interface.ContentCollectionService
  def metadataSetService: interface.metadata.MetadataSetService
  def itemService: interface.item.ItemService

  def isProd: Boolean

  @deprecated("use getDefaultCollection instead", "1.0")
  override def defaultCollection(oid: ObjectId): Option[ObjectId] = {
    getDefaultCollection(oid).right.toOption.map(_.id)
  }

  @deprecated("use getDefaultCollection instead", "1.0")
  override def defaultCollection(o: Organization): Option[ObjectId] = {
    getDefaultCollection(o.id).right.toOption.map(_.id)
  }

  override def addMetadataSet(orgId: ObjectId, setId: ObjectId, checkExistence: Boolean): Either[String, MetadataSetRef] = {
    def applyUpdate = try {
      val ref = MetadataSetRef(setId, true)
      val wr = dao.update(MongoDBObject("_id" -> orgId),
        MongoDBObject("$push" -> MongoDBObject(metadataSets -> grater[MetadataSetRef].asDBObject(ref))),
        false, false)
      if (wr.getLastError.ok()) {
        Right(ref)
      } else {
        Left("error while updating organization data")
      }
    } catch {
      case e: SalatDAOUpdateError => Left("error while updating organization data")
    }

    metadataSetService.findOneById(setId).map(set => applyUpdate)
      .getOrElse {
        if (checkExistence) Left("couldn't find the metadata set") else applyUpdate
      }
  }

  override def updateOrganization(org: Organization): Either[PlatformServiceError, Organization] = {
    val result = changeName(org.id, org.name)
    logger.debug(s"function=updateOrganization, change name result=$result")
    dao.findOneById(org.id) match {
      case None => Left(PlatformServiceError(s"org with id: $org.id not found"))
      case Some(o) => Right(o)
    }
  }

  override def changeName(orgId: ObjectId, name: String): Either[PlatformServiceError, ObjectId] = try {
    logger.debug(s"function=changeName, orgId=$orgId, name=$name")
    val query = MongoDBObject("_id" -> orgId)
    val update = MongoDBObject("$set" -> MongoDBObject(Keys.name -> name))
    val result = dao.update(query, update, false, false, dao.collection.writeConcern)
    if (result.getN == 1) Right(orgId) else Left(PlatformServiceError("Nothing updated"))
  } catch {
    case e: SalatDAOUpdateError => Left(PlatformServiceError("unable to update organization", e))
    case t: Throwable => Left(PlatformServiceError("Error updating", t))
  }

  override def setCollectionEnabledStatus(orgId: ObjectId, collectionId: ObjectId, enabledState: Boolean): Either[PlatformServiceError, ContentCollRef] = {
    val query = MongoDBObject("_id" -> orgId, "contentcolls.collectionId" -> collectionId)
    val update = MongoDBObject("$set" -> MongoDBObject("contentcolls.$.enabled" -> enabledState))
    val projection = MongoDBObject("contentcolls" -> MongoDBObject("$elemMatch" -> MongoDBObject("collectionId" -> collectionId)))

    dao.update(query, update)
    dao.collection.find(query, projection).toSeq match {
      case Seq(dbo) => {
        val dboRef: DBObject = dbo.asInstanceOf[DBObject].get("contentcolls").asInstanceOf[BasicDBList].get(0).asInstanceOf[DBObject]
        val ref: ContentCollRef = com.novus.salat.grater[ContentCollRef].asObject(new MongoDBObject(dboRef))
        Right(ref)
      }
      case _ => Left(PlatformServiceError("organization not found"))
    }
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
  override def removeMetadataSet(orgId: ObjectId, metadataId: ObjectId): Either[PlatformServiceError, MetadataSetRef] =
    findOneById(orgId).map {
      org =>
        val query = MongoDBObject("_id" -> orgId, "metadataSets.metadataId" -> metadataId)
        logger.trace(s"function=removeMetadataSet, query=${query}")
        val pull = MongoDBObject("$pull" -> MongoDBObject("metadataSets" -> MongoDBObject("metadataId" -> metadataId)))
        val result = dao.update(query, pull, false, false, dao.collection.writeConcern)

        logger.debug(s"function=removeMetadataSet, writeResult.getN=${result.getN}")

        if (result.getLastError.ok) {
          if (result.getN != 1) {
            Left(PlatformServiceError("Couldn't remove metadata set from org: $orgId, metadataId: $metadataId"))
          } else {
            logger.trace(s"function=removeMetadataSets, org.metadataSets=${org.metadataSets}, setId=$metadataId")
            val ref = org.metadataSets.find(_.metadataId == metadataId)
            logger.trace(s"function=removeMetadataSets, ref=$ref")
            Right(ref.get)
          }
        } else {
          Left(PlatformServiceError("Error updating orgs"))
        }
    }.getOrElse(Left(PlatformServiceError(("Can't find org with id: " + orgId))))

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
      "_id" -> orgId,
      contentcolls ->
        MongoDBObject(
          "$elemMatch" ->
            MongoDBObject(
              Keys.collectionId -> collectionId,
              pval -> MongoDBObject("$gte" -> permission.value))))

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
  override def insert(org: Organization, optParentId: Option[ObjectId]): Either[PlatformServiceError, Organization] = {

    def update(path: Seq[ObjectId]): Organization = {
      org.copy(
        id = if (isProd) ObjectId.get else org.id,
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
      case Some(id) => Right(org)
      case None => Left(PlatformServiceError("error inserting organization"))
    }
  }

  override def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Either[PlatformServiceError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        dao.update(MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false, false, dao.collection.writeConcern)
        Right(collRef)
      } else {
        Left(PlatformServiceError("collection reference already exists"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(PlatformServiceError(e.getMessage))
    }
  }

  override def getPermissions(orgId: ObjectId, collId: ObjectId): Permission = {
    getTree(orgId).foldRight[Permission](Permission.None)((o, p) => {
      o.contentcolls.find(_.collectionId == collId) match {
        case Some(ccr) => Permission.fromLong(ccr.pval).getOrElse(p)
        case None => p
      }
    })
  }

  override def getDefaultCollection(orgId: ObjectId): Either[PlatformServiceError, ContentCollection] = {
    val collections = collectionService.getCollectionIds(orgId, Permission.Write, false)
    if (collections.isEmpty) {
      collectionService.insertCollection(orgId, ContentCollection(DEFAULT, orgId), Permission.Write)
    } else {
      collectionService.getDefaultCollection(collections) //TODO: Move to collectionService
      //findOne(
      //MongoDBObject("_id" -> MongoDBObject("$in" -> collections), name -> DEFAULT))
      match {
        case Some(default) => Right(default)
        case None =>
          collectionService.insertCollection(orgId, ContentCollection(DEFAULT, orgId), Permission.Write)
      }
    }
  }

  override def findOneById(orgId: ObjectId): Option[Organization] = dao.findOneById(orgId)

  override def findOneByName(name: String): Option[Organization] = dao.findOne(MongoDBObject("name" -> name))

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  override def getTree(parentId: ObjectId): Seq[Organization] = dao.find(MongoDBObject(path -> parentId)).toSeq

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  override def delete(orgId: ObjectId): Either[PlatformServiceError, Unit] = {
    try {
      dao.remove(MongoDBObject("path" -> orgId))
      Right(())
    } catch {
      case e: SalatRemoveError => Left(PlatformServiceError("failed to destroy organization tree", e))
    }
  }

  override def updateCollection(orgId: ObjectId, collRef: ContentCollRef): Either[PlatformServiceError, ContentCollRef] = {
    if (!hasCollRef(orgId, collRef)) {
      Left(PlatformServiceError("can't update collection, it does not exist in this organization"))
    } else {
      // pull the old collection
      try {
        dao.update(
          MongoDBObject("_id" -> orgId),
          MongoDBObject("$pull" -> MongoDBObject(contentcolls -> MongoDBObject("collectionId" -> collRef.collectionId))),
          false,
          false,
          dao.collection.writeConcern)
      } catch {
        case e: SalatDAOUpdateError => Left(PlatformServiceError(e.getMessage))
      }
      // add the updated one
      try {
        dao.update(
          MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false,
          false,
          dao.collection.writeConcern)
      } catch {
        case e: SalatDAOUpdateError => Left(PlatformServiceError(e.getMessage))
      }

      Right(collRef)
    }
  }

  override def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean = {
    dao.findOne(MongoDBObject("_id" -> orgId,
      contentcolls -> MongoDBObject("$elemMatch" ->
        MongoDBObject(Keys.collectionId -> collRef.collectionId, pval -> collRef.pval)))).isDefined
  }

  override def removeCollection(orgId: ObjectId, collId: ObjectId): Either[PlatformServiceError, Unit] = {
    findOneById(orgId) match {
      case Some(org) => {
        val updated = org.copy(contentcolls = org.contentcolls.filter(_.collectionId != collId))
        try {
          dao.update(MongoDBObject("_id" -> orgId), updated, false, false, dao.collection.writeConcern)
          Right(())
        } catch {
          case e: SalatDAOUpdateError => Left(PlatformServiceError(e.getMessage))
        }
      }
      case None => Left(PlatformServiceError("could not find organization"))
    }
  }

  override def deleteCollectionFromAllOrganizations(collId: ObjectId): Either[String, Unit] = {

    def removeCollectionIdFromOrg(): Either[String, Unit] = {
      val query = MongoDBObject(Keys.contentcolls + "." + Keys.collectionId -> collId)
      val update = MongoDBObject("$pull" -> MongoDBObject(Keys.contentcolls -> collId))
      val result = dao.update(query, update, false, true)
      logger.trace(s"function=removeCollectionIdFromOrg, result=$result")
      if (result.getLastError.ok) Right() else Left(s"remove collectionId $collId from orgs failed")
    }

    def removeCollectionIdFromItem(): Validation[String, Unit] = {
      Validation.fromEither(itemService.deleteFromSharedCollections(collId)).leftMap(e => e.message)
    }

    val out: Validation[String, Unit] = for {
      rmFromOrg <- Validation.fromEither(removeCollectionIdFromOrg())
      rmFromItem <- removeCollectionIdFromItem()
    } yield Success()

    out.toEither
  }

  override def addCollectionReference(orgId: ObjectId, reference: ContentCollRef): Either[PlatformServiceError, Unit] = {
    val query = MongoDBObject("_id" -> orgId)
    val update = MongoDBObject("$addToSet" -> MongoDBObject(Keys.contentcolls -> reference))
    val result = dao.update(query, update, false, false)
    if (result.getLastError.ok) Right() else Left(PlatformServiceError(s"Error adding collection reference to org: $orgId, reference: $reference"))
  }

  /**
   * Add the public collection to all orgs to that they have access to it
   * @param collectionId
   * @return
   */
  override def addPublicCollectionToAllOrgs(collectionId: ObjectId): Either[PlatformServiceError, Unit] = {
    val query = MongoDBObject.empty
    val update = MongoDBObject("$addToSet" -> MongoDBObject(Keys.contentcolls -> collectionId))
    val result = dao.update(query, update, false, true)
    if (result.getLastError.ok) Right() else Left(PlatformServiceError(s"Error adding public collection to all orgs $collectionId"))
  }

  override def orgsWithPath(orgId: ObjectId, deep: Boolean): Seq[Organization] = {
    val cursor = if (deep) dao.find(MongoDBObject(path -> orgId)) else dao.find(MongoDBObject("_id" -> orgId)) //find the tree of the given organization
    cursor.toSeq
  }
}
