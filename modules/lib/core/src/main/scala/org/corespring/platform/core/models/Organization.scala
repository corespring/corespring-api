package org.corespring.platform.core.models

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.dao.{ SalatDAOUpdateError, SalatRemoveError, ModelCompanion, SalatDAO }
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.CorespringInternalError
import org.corespring.platform.core.services.metadata.MetadataSetServiceImpl
import org.corespring.platform.core.services.organization.OrganizationService
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import se.radley.plugin.salat._
import search.Searchable

case class Organization(var name: String = "",
                        var path: Seq[ObjectId] = Seq(),
                        var contentcolls: Seq[ContentCollRef] = Seq(),
                        var metadataSets: Seq[MetadataSetRef] = Seq(),
                        var id: ObjectId = new ObjectId()) {

  lazy val isRoot: Boolean = id == AppConfig.rootOrgId

}

trait OrganizationImpl
  extends ModelCompanion[Organization, ObjectId]
  with Searchable
  with OrganizationService
  with PackageLogging {

  val name: String = "name"
  val path: String = "path"
  val contentcolls: String = "contentcolls"
  val id = "id"
  val metadataSets = "metadataSets"

  val collection = mongoCollection("orgs")

  import org.corespring.platform.core.models.mongoContext.context

  val dao = new SalatDAO[Organization, ObjectId](collection = collection) {}

  def apply(): Organization = new Organization();

  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Either[CorespringInternalError, Organization] = {
    if (Play.isProd) org.id = new ObjectId()
    optParentId match {
      case Some(parentId) => {
        findOneById(parentId) match {
          case Some(parent) => {
            org.path = Seq(org.id) ++ parent.path
            org.contentcolls = org.contentcolls ++ ContentCollection.getPublicCollections.map(cc => ContentCollRef(cc.id, Permission.Read.value))
            insert(org) match {
              case Some(id) => Right(org)
              case None => Left(CorespringInternalError("error inserting organization"))
            }
          }
          case None => Left(CorespringInternalError("could not find parent given id"))
        }
      }
      case None => {
        org.path = Seq(org.id)
        org.contentcolls = org.contentcolls ++ ContentCollection.getPublicCollections.map(cc => ContentCollRef(cc.id, Permission.Read.value))
        insert(org) match {
          case Some(id) => Right(org)
          case None => Left(CorespringInternalError("error inserting organization"))
        }
      }
    }
  }

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  def delete(orgId: ObjectId): Either[CorespringInternalError, Unit] = {
    try {
      remove(MongoDBObject(path -> orgId))
      Right(())
    } catch {
      case e: SalatRemoveError => Left(CorespringInternalError("failed to destroy organization tree", e))
    }
  }

  def updateOrganization(org: Organization): Either[CorespringInternalError, Organization] = {
    try {
      update(MongoDBObject("_id" -> org.id), MongoDBObject("$set" -> MongoDBObject(name -> org.name)),
        false, false, collection.writeConcern)
      findOneById(org.id) match {
        case Some(org) => Right(org)
        case None => Left(CorespringInternalError("could not find organization that was just modified"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(CorespringInternalError("unable to update organization", e))
    }
  }

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  def getTree(parentId: ObjectId): Seq[Organization] = find(MongoDBObject(path -> parentId)).toSeq

  def isChild(parentId: ObjectId, childId: ObjectId): Boolean = {
    findOneById(childId) match {
      case Some(child) => {
        if (child.path.size >= 2) child.path(1) == parentId else false
      }
      case None => false
    }
  }

  /**
   * TODO: I'm duplicating hasCollRef, but adjusting the query so that it checks that the stored permission pval is gte than the
   * requested pval.
   * Permissions with a higher pval have access to lower pvals, eg: pval 3 can allow pvals 1,2 and 3.
   * @see: https://www.pivotaltracker.com/s/projects/880382/stories/63449984
   */
  override def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean = {
    def isRequestForPublicCollection(collectionId: ObjectId, permission: Permission) =
      ContentCollection.isPublic(collectionId) && permission == Permission.Read

    val query = MongoDBObject(
      "_id" -> orgId,
      contentcolls ->
        MongoDBObject(
          "$elemMatch" ->
            MongoDBObject(
              ContentCollRef.collectionId -> collectionId,
              ContentCollRef.pval -> MongoDBObject("$gte" -> permission.value))))

    val access = isRequestForPublicCollection(collectionId, permission) || count(query) > 0

    logger.trace(s"[canAccessCollection] orgId: $orgId -> $collectionId ? $access")
    access
  }

  override def canAccessCollection(org: Organization, collectionId: ObjectId, permission: Permission): Boolean = {

    val contentColls = Option(org.contentcolls).getOrElse(Seq[ContentCollRef]())
    val access = contentColls.find(collRef => collRef.collectionId == collectionId && collRef.pval >= permission.value)
      .map(_ => true)
      .getOrElse(ContentCollection.isPublic(collectionId) && permission == Permission.Read)

    logger.trace(s"[canAccessCollection] orgId: ${org.id} -> $collectionId ? $access")
    access
  }

  def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean = {
    findOne(MongoDBObject("_id" -> orgId,
      contentcolls -> MongoDBObject("$elemMatch" ->
        MongoDBObject(ContentCollRef.collectionId -> collRef.collectionId, ContentCollRef.pval -> collRef.pval)))).isDefined
  }

  def removeCollection(orgId: ObjectId, collId: ObjectId): Either[CorespringInternalError, Unit] = {
    findOneById(orgId) match {
      case Some(org) => {
        org.contentcolls = org.contentcolls.filter(_.collectionId != collId)
        try {
          update(MongoDBObject("_id" -> orgId), org, false, false, defaultWriteConcern)
          Right(())
        } catch {
          case e: SalatDAOUpdateError => Left(CorespringInternalError(e.getMessage))
        }
      }
      case None => Left(CorespringInternalError("could not find organization"))
    }
  }

  def getPermissions(orgId: ObjectId, collId: ObjectId): Permission = {
    getTree(orgId).foldRight[Permission](Permission.None)((o, p) => {
      o.contentcolls.find(_.collectionId == collId) match {
        case Some(ccr) => Permission.fromLong(ccr.pval).getOrElse(p)
        case None => p
      }
    })
  }

  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Either[CorespringInternalError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        update(MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false, false, collection.writeConcern)
        Right(collRef)
      } else {
        Left(CorespringInternalError("collection reference already exists"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(CorespringInternalError(e.getMessage))
    }
  }

  def setCollectionEnabledStatus(orgId: ObjectId, collectionId: ObjectId, enabledState: Boolean): Either[CorespringInternalError, ContentCollRef] = {
    val orgOpt = findOneById(orgId)
    orgOpt match {
      case Some(org) =>
        val collrefOpt = org.contentcolls.find(ref => ref.collectionId == collectionId)
        collrefOpt match {
          case Some(ref) =>
            ref.enabled = enabledState
            updateCollection(orgId, ref)
          case None => Left(CorespringInternalError("collection reference not found"))
        }

      case None => Left(CorespringInternalError("organization not found"))
    }
  }

  def updateCollection(orgId: ObjectId, collRef: ContentCollRef): Either[CorespringInternalError, ContentCollRef] = {
    if (!hasCollRef(orgId, collRef)) {
      Left(CorespringInternalError("can't update collection, it does not exist in this organization"))
    } else {
      // pull the old collection
      try {
        update(
          MongoDBObject("_id" -> orgId),
          MongoDBObject("$pull" -> MongoDBObject(contentcolls -> MongoDBObject("collectionId" -> collRef.collectionId))),
          false,
          false,
          collection.writeConcern)
      } catch {
        case e: SalatDAOUpdateError => Left(CorespringInternalError(e.getMessage))
      }
      // add the updated one
      try {
        update(
          MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false,
          false,
          collection.writeConcern)
      } catch {
        case e: SalatDAOUpdateError => Left(CorespringInternalError(e.getMessage))
      }

      Right(collRef)
    }
  }

  override def getDefaultCollection(orgId: ObjectId): Either[CorespringInternalError, ContentCollection] = {
    val collections = ContentCollection.getCollectionIds(orgId, Permission.Write, false);
    if (collections.isEmpty) {
      ContentCollection.insertCollection(orgId, ContentCollection(ContentCollection.DEFAULT, orgId), Permission.Write);
    } else {
      ContentCollection.findOne(
        MongoDBObject("_id" -> MongoDBObject("$in" -> collections), ContentCollection.name -> ContentCollection.DEFAULT)) match {
          case Some(default) => Right(default)
          case None =>
            ContentCollection.insertCollection(orgId, ContentCollection(ContentCollection.DEFAULT, orgId), Permission.Write);
        }
    }
  }

  def addMetadataSet(orgId: ObjectId, msId: ObjectId, checkExistence: Boolean = true): Either[String, MetadataSetRef] = {

    def applyUpdate = try {
      val ref = MetadataSetRef(msId, true)
      val wr = update(MongoDBObject("_id" -> orgId),
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

    metadataSetService.findOneById(msId).map(set => applyUpdate)
      .getOrElse {
        if (checkExistence) Left("couldn't find the metadata set") else applyUpdate
      }
  }

  def removeMetadataSet(orgId: ObjectId, setId: ObjectId): Option[String] = findOneById(orgId).map {
    org =>
      val query = MongoDBObject("_id" -> orgId)
      val pull = MongoDBObject("$pull" -> MongoDBObject("metadataSets" -> MongoDBObject("metadataId" -> setId)))
      val result = update(query, pull, false, false, collection.writeConcern)
      if (result.getLastError.ok) None else Some("Error updating orgs")
  }.getOrElse(Some("Can't find org with id: " + orgId))

  object FullWrites extends BasicWrites {

    implicit object CollectionReferenceWrites extends Writes[ContentCollRef] {
      def writes(ref: ContentCollRef) = {
        JsObject(
          Seq(
            "collectionId" -> JsString(ref.collectionId.toString),
            "permission" -> JsString(Permission.toHumanReadable(ref.pval)),
            "enabled" -> JsBoolean(ref.enabled)))
      }
    }

    override def writes(org: Organization) = {
      val jsObject = super.writes(org)
      jsObject ++ JsObject(Seq("collections" -> Json.toJson(org.contentcolls)))
    }
  }

  implicit object OrganizationWrites extends BasicWrites

  class BasicWrites extends Writes[Organization] {
    def writes(org: Organization) = {
      var list = List[(String, JsValue)]()
      if (org.path.nonEmpty) list = ("path" -> JsArray(org.path.map(c => JsString(c.toString)).toSeq)) :: list
      if (org.name.nonEmpty) list = ("name" -> JsString(org.name)) :: list
      list = ("isRoot" -> JsBoolean(org.isRoot)) :: list
      list = ("id" -> JsString(org.id.toString)) :: list
      JsObject(list)
    }
  }

  override val searchableFields = Seq(name)

}

object Organization extends OrganizationImpl {

  implicit class Accessible(collections: Seq[ContentCollRef]) {
    private val readable = (collection: ContentCollRef) => (collection.pval > 0 && collection.enabled == true)
    def accessible = collections.filter(readable)
  }

  def metadataSetService: MetadataSetServiceImpl = new MetadataSetServiceImpl {
    def orgService: OrganizationService = Organization
  }
}

case class ContentCollRef(var collectionId: ObjectId, var pval: Long = Permission.Read.value, var enabled: Boolean = false)
object ContentCollRef {
  val pval: String = "pval"
  val collectionId: String = "collectionId"
  val enabled: String = "enabled"
}

case class MetadataSetRef(var metadataId: ObjectId, var isOwner: Boolean)

