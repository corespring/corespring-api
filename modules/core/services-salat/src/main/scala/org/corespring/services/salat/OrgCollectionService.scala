package org.corespring.services.salat

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.services.CollectionIdPermission
import org.corespring.services.salat.OrgCollectionService.OrgKeys
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.omg.CORBA.TIMEOUT

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
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
  salatServicesExecutionContext: SalatServicesExecutionContext,
  implicit val context: Context) extends org.corespring.services.OrgCollectionService {

  private val TIMEOUT = 5.seconds

  implicit val ec: ExecutionContext = salatServicesExecutionContext.ctx

  private val logger = Logger(classOf[OrgCollectionService])

  override def listAllCollectionsAvailableForOrg(orgId: ObjectId, skip: Int, limit: Int): Future[Stream[CollectionInfo]] = {

    def listCollectionsByOrg(refs: Seq[ContentCollRef]): Stream[ContentCollection] = {
      val collectionIds = refs.map(_.collectionId)
      val query = ("_id" $in collectionIds)
      logger.trace(s"function=listCollectionsByOrg, orgId=$orgId, query=$query, skip=$skip, limit=$limit")
      collectionDao.find(query).skip(skip).limit(limit).toStream
    }

    logger.trace(s"function=listAllCollectionsAvailableForOrg, orgId=$orgId")
    val refs = getContentCollRefs(orgId, Permission.Read)
    val collections = listCollectionsByOrg(refs)
      .filterNot(_.id == collectionService.archiveCollectionId)

    logger.trace(s"function=listAllCollectionsAvailableForOrg, orgId=$orgId - call count")
    itemService.countItemsInCollections(collections.map(_.id): _*).map { counts =>
      collections.flatMap { c =>
        for {
          p <- refs.find(r => r.collectionId == c.id).flatMap(r => Permission.fromLong(r.pval))
          count <- counts.find(_.collectionId == c.id).map(_.count)
        } yield CollectionInfo(c, count, orgId, p)
      }
    }

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

  override def enableOrgAccessToCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, true)
  }

  override def disableOrgAccessToCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef] = {
    toggleCollectionEnabled(orgId, collectionId, false)
  }

  private def toggleCollectionEnabled(orgId: ObjectId, collectionId: ObjectId, enabled: Boolean): Validation[PlatformServiceError, ContentCollRef] = {
    val query = MongoDBObject(OrgKeys.id -> orgId, "contentcolls.collectionId" -> collectionId)
    val update = MongoDBObject("$set" -> MongoDBObject("contentcolls.$.enabled" -> enabled))

    val res = orgDao.update(query, update)
    if (res.getN == 1) getCollRef(orgId, collectionId) else Failure(PlatformServiceError("Nothing updated"))
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

    for {
      _ <- collectionDao.findOneById(collId).toSuccess(PlatformServiceError(s"Can't find a collection with the id: $collId"))
      o <- orgDao.findOneById(orgId)
        .map(updateOrAddNewReference)
        .toSuccess(PlatformServiceError(s"Can't find org with id: $orgId"))
    } yield o
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
        findDefaultCollection(org.contentcolls.map(_.collectionId)) match {
          case Some(default) => Success(default)
          case None =>
            collectionService.insertCollection(
              ContentCollection(ContentCollection.Default, orgId))
        }
      }
    }
  }

  /** Get a default collection from the set of ids */
  private def findDefaultCollection(collections: Seq[ObjectId]): Option[ContentCollection] = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> collections), "name" -> "default")
    collectionDao.findOne(query)
  }

  override def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean = isAuthorized(orgId, CollectionIdPermission(collId, p))

  override def isAuthorized(orgId: ObjectId, collectionIdPermission: CollectionIdPermission): Boolean = {
    logger.debug(s"function=isAuthorized, orgId=$orgId, collId=$collectionIdPermission")
    val batchResult = Await.result(isAuthorizedBatch(orgId, collectionIdPermission), TIMEOUT)
    logger.trace(s"function=isAuthorized, batchResult=$batchResult")
    val maybeAuthorized = batchResult.find { { case (id, _) => id == collectionIdPermission } }
    logger.trace(s"function=isAuthorized, maybeAuthorized=$maybeAuthorized")
    maybeAuthorized.map(_._2).getOrElse(false)
  }

  override def isAuthorizedBatch(orgId: ObjectId, idsAndPermissions: CollectionIdPermission*): Future[Seq[(CollectionIdPermission, Boolean)]] = {
    logger.trace(s"function=isAuthorizedBatch, idsAndPermissions=$idsAndPermissions")

    getPermissions(orgId, idsAndPermissions.map(_.collectionId).distinct: _*).map { grantedPermissions =>

      logger.trace(s"grantedPermissions: ${grantedPermissions}")
      val out = idsAndPermissions.map { idPerm =>
        val granted = grantedPermissions
          .find(_._1 == idPerm.collectionId)
          .getOrElse(idPerm.collectionId -> None)

        logger.trace(s"granted: $granted")
        logger.trace(s"idPerm: $idPerm")

        val authorized = granted._2.map(_.has(idPerm.permission)).getOrElse(false)
        logger.trace(s"authorized:: $authorized")
        idPerm -> authorized
      }

      logger.trace(s"out: $out")
      out
    }
  }

  override def getPermission(orgId: ObjectId, collId: ObjectId): Option[Permission] = {
    val perms = Await.result(getPermissions(orgId, collId), TIMEOUT)
    perms.find({ case (id, p) => id == collId }).flatMap(_._2)
  }

  override def getPermissions(orgId: ObjectId, collectionIds: ObjectId*): Future[Stream[(ObjectId, Option[Permission])]] = Future {
    logger.debug(s"function=getPermissions, orgId=$orgId, collectionIds=$collectionIds")

    val distinctIds = collectionIds.distinct

    lazy val stream = orgService.orgsWithPath(orgId, true)
    lazy val allRefs = stream.map(_.contentcolls).flatten.distinct
    logger.trace(s"function=getPermissions, allRefs=$allRefs")

    val query = "_id" $in distinctIds

    def isArchiveCollection(c: ContentCollection) = collectionService.archiveCollectionId == c.id

    def permissionFromRef(c: ContentCollection): (ObjectId, Option[Permission]) = {
      val fallbackPermission = if ((c.isPublic) || (isArchiveCollection(c))) Some(Permission.Read) else None
      val p = allRefs.find(_.collectionId == c.id).flatMap { r => Permission.fromLong(r.pval) }.orElse(fallbackPermission)
      c.id -> p
    }

    val foundIdPermissions = collectionDao.find(query).toStream.map(permissionFromRef)
    val notFoundIds = distinctIds.filterNot(id => foundIdPermissions.exists(_._1 == id))
    val out = foundIdPermissions ++ notFoundIds.map(_ -> None)
    logger.trace(s"function=getPermissions, out=$out")
    out
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

}
