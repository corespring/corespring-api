package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.{ SalatDAOUpdateError, SalatDAO }
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.corespring.services.errors.PlatformServiceError
import org.corespring.services.salat.OrgCollectionService.OrgKeys

import scalaz.{ Success, Validation, Failure }

object OrgCollectionService {

  object OrgKeys {
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
}

class OrgCollectionService(
  orgDao: SalatDAO[Organization, ObjectId],
  collectionDao: SalatDAO[ContentCollection, ObjectId]) extends org.corespring.services.OrgCollectionService {

  override def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]] = {
    val collectionIds = getCollectionIds(orgId, p, deep = false)
    Success(collectionDao.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collectionIds))).toSeq)
  }

  private def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef] = {

    val orgs = orgsWithPath(orgId, deep)

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
  override def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId] = getContentCollRefs(orgId, p, deep).map(_.collectionId)
  /** Enable this collection for this org */
  override def enableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    enableCollection(orgId, collectionId)
  }

  /** Enable the collection for the org */
  override def disableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    disableCollection(orgId, collectionId)
  }

  /** Enable this collection for this org */
  override def enableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, true)
  }

  /** Enable the collection for the org */
  override def disableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, false)
  }

  /**
   * TODO: I'm using hasCollRef with $gte, so that it checks that the stored permission
   * pval is gte than the requested pval.
   * Permissions with a higher pval have access to lower pvals, eg: pval 3 can allow pvals 1,2 and 3.
   * see: https://www.pivotaltracker.com/s/projects/880382/stories/63449984
   */
  override def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean = {

    def isRequestForPublicCollection() =
      collectionService.isPublic(collectionId) && permission == Permission.Read

    def hasMatchingCollection() = hasCollRef(orgId, collectionId, permission.value, "$gte")

    val access = isRequestForPublicCollection() || hasMatchingCollection()
    logger.trace(s"[canAccessCollection] orgId: $orgId -> $collectionId ? $access")
    access
  }

  override def canAccessCollection(org: Organization, collectionId: ObjectId, permission: Permission): Boolean =
    canAccessCollection(org.id, collectionId, permission)

  override def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        addCollectionReference(orgId, collRef).map(_ => collRef)
      } else {
        Failure(PlatformServiceError("collection reference already exists"))
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  override def addCollectionReference(orgId: ObjectId, collRef: ContentCollRef): Validation[PlatformServiceError, Unit] = {
    val query = MongoDBObject(OrgKeys.id -> orgId)
    addCollRefToOrgs(query, collRef)
  }

  private def removeCollRefFromOrgs(orgsQuery: DBObject, collRef: ContentCollRef, multi: Boolean): Validation[PlatformServiceError, Unit] = {
    removeCollRefFromOrgs(orgsQuery, collRef.collectionId, multi)
  }

  private def addCollRefToOrgs(orgsQuery: DBObject, collRef: ContentCollRef, multi: Boolean = false) = {
    try {
      val update = MongoDBObject("$addToSet" -> MongoDBObject(OrgKeys.contentcolls -> com.novus.salat.grater[ContentCollRef].asDBObject(collRef)))
      val result = orgDao.update(orgsQuery, update, upsert = false, multi = multi, dao.collection.writeConcern)
      if (result.getLastError.ok) Success() else Failure(PlatformServiceError(s"Error adding collection reference to orgs: $orgsQuery, reference: $collRef"))
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  private def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean =
    hasCollRef(orgId, collRef.collectionId, collRef.pval, "$eq")

  private def hasCollRef(orgId: ObjectId, collectionId: ObjectId, pval: Long, pCompareOp: String): Boolean = {
    orgDao.findOne(
      MongoDBObject("_id" -> orgId,
        OrgKeys.contentcolls -> MongoDBObject("$elemMatch" ->
          MongoDBObject(
            OrgKeys.collectionId -> collectionId,
            OrgKeys.pval -> MongoDBObject(pCompareOp -> pval))))).isDefined
  }

  private def removeCollRefFromOrgs(orgsQuery: DBObject, collectionId: ObjectId, multi: Boolean) = {
    try {
      val update = MongoDBObject("$pull" -> MongoDBObject(OrgKeys.contentcolls -> MongoDBObject(OrgKeys.collectionId -> collectionId)))
      val result = orgDao.update(orgsQuery, update, upsert = false, multi = multi, orgDao.collection.writeConcern)
      if (result.getLastError.ok) Success() else Failure(PlatformServiceError(s"Error removing collection from orgs: $orgsQuery, collection id: $collectionId"))
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  /**
   * Add the public collection to all orgs so that they have access to it
   * @param collectionId
   * @return
   */
  //TODO: Adding a content collection with isPublic=true is all that is required to allow access to that collection
  //Is adding a Ref needed?
  @deprecated("There is no need to add the ref to all orgs")
  override def addPublicCollectionToAllOrgs(collectionId: ObjectId): Validation[PlatformServiceError, Unit] = {
    collectionService.findOneById(collectionId).map { c =>
      val query = MongoDBObject.empty
      val collRef = ContentCollRef(collectionId, Permission.Read.value, true)
      removeCollRefFromOrgs(query, collRef, true)
      addCollRefToOrgs(query, collRef, true)
    }.getOrElse(Failure(PlatformServiceError(s"collection does not exist: $collectionId")))
  }

  override def removeCollection(orgId: ObjectId, collId: ObjectId): Validation[PlatformServiceError, Unit] = {
    orgDao.findOneById(orgId) match {
      case Some(org) => {
        removeCollRefFromOrgs(MongoDBObject(OrgKeys.id -> orgId), collId, false)
      }
      case None => Failure(PlatformServiceError("could not find organization"))
    }
  }

  override def deleteCollectionFromAllOrganizations(collId: ObjectId): Validation[String, Unit] = {

    def removeCollectionIdFromOrgs() = {
      removeCollRefFromOrgs(MongoDBObject.empty, collId, true).leftMap(e => e.message)
    }

    def removeCollectionIdFromItem() = {
      itemService.deleteFromSharedCollections(collId).leftMap(e => e.message)
    }

    for {
      rmFromOrg <- removeCollectionIdFromOrgs()
      rmFromItem <- removeCollectionIdFromItem()
    } yield Success()
  }

  override def orgsWithPath(orgId: ObjectId, deep: Boolean): Seq[Organization] = {
    val cursor = if (deep) orgDao.find(MongoDBObject(OrgKeys.path -> orgId)) else orgDao.find(MongoDBObject(OrgKeys.id -> orgId)) //find the tree of the given organization
    cursor.toSeq
  }

  override def getOrCreateDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection] = {
    orgDao.findOneById(orgId) match {
      case None => Failure(PlatformServiceError(s"Org not found $orgId"))
      case Some(org) => {
        val collections = getCollectionIds(orgId, Permission.Write, false)
        if (collections.isEmpty) {
          collectionService.insertCollection(orgId, ContentCollection(Keys.DEFAULT, orgId), Permission.Write)
        } else {
          collectionService.getDefaultCollection(collections) match {
            case Some(default) => Success(default)
            case None =>
              collectionService.insertCollection(orgId, ContentCollection(Keys.DEFAULT, orgId), Permission.Write)
          }
        }
      }
    }
  }

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  override def getTree(parentId: ObjectId): Seq[Organization] = orgDao.find(MongoDBObject(Keys.path -> parentId)).toSeq

  override def getPermissions(orgId: ObjectId, collId: ObjectId): Option[Permission] = {
    getTree(orgId).foldRight[Option[Permission]](None)((o, p) => {
      o.contentcolls.find(_.collectionId == collId) match {
        case Some(ccr) => Permission.fromLong(ccr.pval)
        case None => p
      }
    })
  }
  private def toggleCollectionEnabled(orgId: ObjectId, collectionId: ObjectId, enabled: Boolean): Validation[PlatformServiceError, ContentCollRef] = {
    val query = MongoDBObject(OrgKeys.id -> orgId, "contentcolls.collectionId" -> collectionId)
    val update = MongoDBObject("$set" -> MongoDBObject("contentcolls.$.enabled" -> enabled))

    val res = orgDao.update(query, update)
    if (res.getN == 1) getCollRef(orgId, collectionId) else Failure(PlatformServiceError("Nothing updated"))
  }

  private def getCollRef(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    import scalaz.Scalaz._

    val query = MongoDBObject(OrgKeys.id -> orgId)
    val projection = MongoDBObject(OrgKeys.contentcolls -> MongoDBObject("$elemMatch" -> MongoDBObject(Keys.collectionId -> collectionId)))

    def getRef(dbo: DBObject) = {
      import org.corespring.common.mongo.ExpandableDbo.ExpandableDbo
      for {
        dboRef <- dbo.expandPath(OrgKeys.contentcolls + ".0")
        ref <- Some(com.novus.salat.grater[ContentCollRef].asObject(new MongoDBObject(dboRef)))
      } yield ref
    }

    orgDao.collection.find(query, projection).toSeq match {
      case Seq(dbo) => getRef(dbo).toSuccess(PlatformServiceError("Organization does not have collection"))
      case _ => Failure(PlatformServiceError("Organization not found"))
    }
  }

  override def getOrgsWithAccessTo(collectionId: ObjectId): Stream[Organization] = {
    val query = MongoDBObject("contentcolls.collectionId" -> MongoDBObject("$in" -> List(collectionId)))
    orgDao.find(query).toStream
  }
}
