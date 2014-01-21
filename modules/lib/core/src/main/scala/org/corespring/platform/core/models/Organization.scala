package org.corespring.platform.core.models

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.dao.{ SalatDAOUpdateError, SalatRemoveError, ModelCompanion, SalatDAO }
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
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

trait OrganizationImpl extends ModelCompanion[Organization, ObjectId] with Searchable with OrganizationService {

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
  def insert(org: Organization, optParentId: Option[ObjectId]): Either[InternalError, Organization] = {
    if (Play.isProd) org.id = new ObjectId()
    optParentId match {
      case Some(parentId) => {
        findOneById(parentId) match {
          case Some(parent) => {
            org.path = Seq(org.id) ++ parent.path
            org.contentcolls = org.contentcolls ++ ContentCollection.getPublicCollections.map(cc => ContentCollRef(cc.id, Permission.Read.value))
            insert(org) match {
              case Some(id) => Right(org)
              case None => Left(InternalError("error inserting organization"))
            }
          }
          case None => Left(InternalError("could not find parent given id"))
        }
      }
      case None => {
        org.path = Seq(org.id)
        org.contentcolls = org.contentcolls ++ ContentCollection.getPublicCollections.map(cc => ContentCollRef(cc.id, Permission.Read.value))
        insert(org) match {
          case Some(id) => Right(org)
          case None => Left(InternalError("error inserting organization"))
        }
      }
    }
  }

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  def delete(orgId: ObjectId): Either[InternalError, Unit] = {
    try {
      remove(MongoDBObject(path -> orgId))
      Right(())
    } catch {
      case e: SalatRemoveError => Left(InternalError("failed to destroy organization tree", e))
    }
  }

  def updateOrganization(org: Organization): Either[InternalError, Organization] = {
    try {
      update(MongoDBObject("_id" -> org.id), MongoDBObject("$set" -> MongoDBObject(name -> org.name)),
        false, false, collection.writeConcern)
      findOneById(org.id) match {
        case Some(org) => Right(org)
        case None => Left(InternalError("could not find organization that was just modified"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("unable to update organization", e))
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

  def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean = {
    findOne(MongoDBObject("_id" -> orgId,
      contentcolls -> MongoDBObject("$elemMatch" ->
        MongoDBObject(ContentCollRef.collectionId -> collRef.collectionId, ContentCollRef.pval -> collRef.pval)))).isDefined
  }
  def removeCollection(orgId: ObjectId, collId: ObjectId): Either[InternalError, Unit] = {
    findOneById(orgId) match {
      case Some(org) => {
        org.contentcolls = org.contentcolls.filter(_.collectionId != collId)
        try {
          update(MongoDBObject("_id" -> orgId), org, false, false, defaultWriteConcern)
          Right(())
        } catch {
          case e: SalatDAOUpdateError => Left(InternalError(e.getMessage))
        }
      }
      case None => Left(InternalError("could not find organization"))
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
  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Either[InternalError, ContentCollRef] = {
    try {
      val collRef = new ContentCollRef(collId, p.value)
      if (!hasCollRef(orgId, collRef)) {
        update(MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false, false, collection.writeConcern)
        Right(collRef)
      } else {
        Left(InternalError("collection reference already exists"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage))
    }
  }

  def setCollectionEnabledStatus(orgId: ObjectId, collectionId: ObjectId, enabledState: Boolean) : Either[InternalError, ContentCollRef] = {
    val orgOpt = findOneById(orgId)
    orgOpt match {
      case Some(org) =>
        val collrefOpt = org.contentcolls.find(ref => ref.collectionId == collectionId)
        collrefOpt match {
          case Some(ref) =>
            ref.enabled = enabledState
            updateCollection(orgId, ref)
          case None => Left(InternalError("collection reference not found"))
        }

      case None => Left(InternalError("organization not found"))
    }
  }

  def updateCollection(orgId: ObjectId, collRef: ContentCollRef) : Either[InternalError, ContentCollRef] = {
    if (!hasCollRef(orgId, collRef)) {
      Left(InternalError("can't update collection, it does not exist in this organization"))
    } else {
      // pull the old collection
      try {
        update(
          MongoDBObject("_id" -> orgId),
          MongoDBObject("$pull" -> MongoDBObject(contentcolls -> MongoDBObject("collectionId" -> collRef.collectionId)) ),
          false,
          false,
          collection.writeConcern
        )
      } catch {
        case e: SalatDAOUpdateError => Left(InternalError(e.getMessage))
      }
      // add the updated one
      try {
        update(
          MongoDBObject("_id" -> orgId),
          MongoDBObject("$addToSet" -> MongoDBObject(contentcolls -> grater[ContentCollRef].asDBObject(collRef))),
          false,
          false,
          collection.writeConcern
        )
      } catch {
        case e: SalatDAOUpdateError => Left(InternalError(e.getMessage))
      }

      Right(collRef)
    }
  }

  def getDefaultCollection(orgId: ObjectId): Either[InternalError, ContentCollection] = {
    val collections = ContentCollection.getCollectionIds(orgId, Permission.Write, false);
    if (collections.isEmpty) {
      ContentCollection.insertCollection(orgId, ContentCollection(ContentCollection.DEFAULT, orgId.toString() ), Permission.Write);
    } else {
      ContentCollection.findOne(
        MongoDBObject("_id" -> MongoDBObject("$in" -> collections), ContentCollection.name -> ContentCollection.DEFAULT)) match {
          case Some(default) => Right(default)
          case None =>
            ContentCollection.insertCollection(orgId, ContentCollection(ContentCollection.DEFAULT, orgId.toString() ), Permission.Write);
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

  def removeMetadataSet(orgId: ObjectId, setId: ObjectId): Option[String] = findOneById(orgId).map { org =>
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
            "enabled" -> JsBoolean(ref.enabled)
          )
        )
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
  override val searchableFields = Seq(
    name)

}

object Organization extends OrganizationImpl {
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

