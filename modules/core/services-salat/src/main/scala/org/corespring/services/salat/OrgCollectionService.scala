package org.corespring.services.salat

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.services.errors.PlatformServiceError
import org.corespring.services.salat.OrgCollectionService.OrgKeys

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

object OrgCollectionService {

  private[OrgCollectionService] object OrgKeys {
    val collectionId = "collectionId"
    val contentcolls = "contentcolls"
    val id = "_id"
    val pval = "pval"
  }
}

class OrgCollectionService(orgService: => org.corespring.services.OrganizationService,
  collectionService: => org.corespring.services.ContentCollectionService,
  itemService: org.corespring.services.item.ItemService,
  orgDao: SalatDAO[Organization, ObjectId],
  collectionDao: SalatDAO[ContentCollection, ObjectId],
  implicit val context: Context) extends org.corespring.services.OrgCollectionService {

  private val logger = Logger(classOf[OrgCollectionService])

  override def listAllCollectionsAvailableForOrg(orgId: ObjectId): Stream[CollectionInfo] = {

    logger.trace(s"function=listAllCollectionsAvailableForOrg, orgId=$orgId")
    val refs = getContentCollRefs(orgId, Permission.Read)
    listCollectionsByOrg(orgId)
      .filterNot(_.id == collectionService.archiveCollectionId)
      .flatMap { c =>
        val permission = refs.find(r => r.collectionId == c.id).flatMap(r => Permission.fromLong(r.pval))

        permission.map(p =>
          CollectionInfo(c, collectionService.itemCount(c.id), orgId, p))
      }
  }

  private def listCollectionsByOrg(orgId: ObjectId): Stream[ContentCollection] = {
    val refs = getContentCollRefs(orgId, Permission.Read).map(_.collectionId)
    val query = ("_id" $in refs)
    logger.trace(s"function=listCollectionsByOrg, orgId=$orgId, query=$query")
    collectionDao.find(query).toStream
  }

  override def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Boolean] = {
    for {
      collection <- collectionService.findOneById(collectionId).toSuccess(PlatformServiceError(s"Cant find collection with id: $collectionId"))
      result <- if (collection.ownerOrgId == org.id) Success() else Failure(PlatformServiceError(s"Organisation ${org.name} does not own collection: $collectionId."))
    } yield true
  }

  override def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]] = {
    val collectionIds = getCollectionIds(orgId, p)
    Success(collectionDao.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collectionIds))).toSeq)
  }

  private def getContentCollRefs(orgId: ObjectId, p: Permission): Seq[ContentCollRef] = {

    logger.debug(s"function=getContentCollRefs, orgId=$orgId, p=$p")
    val orgs = orgService.orgsWithPath(orgId, deep = true)

    logger.trace(s"function=getContentCollRefs, orgId=$orgId, orgs.size=${orgs.size}")

    def addRefsWithPermission(org: Organization, acc: Seq[ContentCollRef]): Seq[ContentCollRef] = {
      acc ++ org.contentcolls.filter(ref => (ref.pval & p.value) == p.value)
    }

    val out = orgs.foldRight[Seq[ContentCollRef]](Seq.empty)(addRefsWithPermission)
    logger.trace(s"function=getContentCollRefs, refs=$out")
    if (p == Permission.Read) {
      out ++ collectionService.getPublicCollections.map(c => ContentCollRef(c.id, Permission.Read.value, enabled = true))
    } else {
      out
    }
  }

  private def getCollectionIds(orgId: ObjectId, p: Permission): Seq[ObjectId] = getContentCollRefs(orgId, p).map(_.collectionId)

  /** Enable this collection for this org */
  override def enableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, true)
  }

  /** Enable the collection for the org */
  override def disableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, false)
  }

  override def grantAccessToCollection(orgId: Imports.ObjectId, collId: Imports.ObjectId, p: Permission): Validation[PlatformServiceError, Organization] = {

    logger.debug(s"function=grantAccessToCollection, orgId=$orgId, collId=$collId, p=$p")
    def updateOrAddNewReference(o: Organization): Organization = {
      val ref = o.contentcolls
        .find(_.collectionId == collId)
        .map(r => r.copy(pval = p.value))
        .getOrElse(ContentCollRef(collId, p.value, true))

      val updatedRefs = if (o.contentcolls.exists(_.collectionId == collId)) {
        o.contentcolls.map { r =>
          if (r.collectionId == collId) ref else r
        }
      } else {
        o.contentcolls :+ ref
      }

      val updatedOrg = o.copy(contentcolls = updatedRefs)

      orgDao.save(updatedOrg)
      updatedOrg
    }

    orgDao.findOneById(orgId)
      .map(updateOrAddNewReference)
      .toSuccess(PlatformServiceError(s"Can't find org with id: $orgId"))
  }

  override def removeAccessToCollection(orgId: Imports.ObjectId, collId: Imports.ObjectId): Validation[PlatformServiceError, Organization] = {
    orgDao.findOneById(orgId).map { o =>
      val updatedOrg = o.copy(contentcolls = o.contentcolls.filterNot(_.collectionId == collId))
      orgDao.save(updatedOrg)
      updatedOrg
    }.toSuccess(PlatformServiceError(s"Can't find org with Id: $orgId"))
  }

  override def removeAllAccessToCollection(collId: Imports.ObjectId): Validation[PlatformServiceError, Unit] = Validation.fromTryCatch {
    val update = MongoDBObject("$pull" -> MongoDBObject(OrgKeys.contentcolls -> MongoDBObject(OrgKeys.collectionId -> collId)))
    val result = orgDao.update(MongoDBObject.empty, update, upsert = false, multi = true, orgDao.collection.writeConcern)
    if (!result.getLastError.ok) {
      throw new RuntimeException("Remove failed")
    }
  }.leftMap(t => PlatformServiceError("Remove failed", t))

  override def getDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection] = {
    orgDao.findOneById(orgId) match {
      case None => Failure(PlatformServiceError(s"Org not found $orgId"))
      case Some(org) => {
        collectionService.getDefaultCollection(org.contentcolls.map(_.collectionId)) match {
          case Some(default) => Success(default)
          case None =>
            collectionService.insertCollection(
              ContentCollection(ContentCollection.Default, orgId))
        }
      }
    }
  }

  override def getPermission(orgId: ObjectId, collId: ObjectId): Option[Permission] = {
    logger.debug(s"fuction=getPermission, orgId=$orgId, collId=$collId")

    val stream = orgService.orgsWithPath(orgId, true)

    lazy val publicPermission = collectionDao.findOneById(collId).flatMap { c =>
      if (c.isPublic) Some(Permission.Read) else None
    }

    val allRefs = stream.map(_.contentcolls).flatten.distinct

    logger.trace(s"function=getPermission, allRefs=$allRefs")

    if (allRefs.isEmpty) {
      publicPermission
    } else {
      allRefs.find(_.collectionId == collId).map { r =>
        Permission.fromLong(r.pval)
      }.getOrElse(publicPermission)
    }
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
    val projection = MongoDBObject(OrgKeys.contentcolls -> MongoDBObject("$elemMatch" -> MongoDBObject(OrgKeys.collectionId -> collectionId)))

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

  override def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean = {
    getPermission(orgId, collId).map { permissionForOrg =>
      logger.trace(s"function=isAuthorized, permissionForOrg=$permissionForOrg, p=$p")
      permissionForOrg.has(p)
    }.getOrElse(false)
  }

}
