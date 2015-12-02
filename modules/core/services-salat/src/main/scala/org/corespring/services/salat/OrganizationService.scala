package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.{ SalatDAO, SalatRemoveError }
import com.novus.salat.{ Context, grater }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, MetadataSetRef, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.{ services => interface }

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

object OrganizationService {

  object Keys {
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

class OrganizationService(
  val dao: SalatDAO[Organization, ObjectId],
  implicit val context: Context,
  orgCollectionService: interface.OrgCollectionService,
  collectionService: => interface.ContentCollectionService,
  metadataSetService: interface.metadata.MetadataSetService,
  itemService: interface.item.ItemService) extends interface.OrganizationService with HasDao[Organization, ObjectId] {

  lazy val logger: Logger = Logger(classOf[OrganizationService])

  import OrganizationService.Keys

  override def addMetadataSet(orgId: ObjectId, setId: ObjectId): Validation[String, MetadataSetRef] = {
    val ref = MetadataSetRef(setId, true)
    val wr = dao.update(
      MongoDBObject(Keys.id -> orgId),
      MongoDBObject("$push" -> MongoDBObject(Keys.metadataSets -> grater[MetadataSetRef].asDBObject(ref))),
      false, false)
    if (wr.getN == 1) Success(ref) else Failure("Error while updating organization $orgId with metadata set $setId")
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
        logger.trace(s"function=removeMetadataSet, orgsQuery=${query}")
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
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  override def insert(org: Organization, optParentId: Option[ObjectId]): Validation[PlatformServiceError, Organization] = {

    def addPaths(paths: Seq[ObjectId]): Organization = {
      org.copy(
        id = org.id,
        path = Seq(org.id) ++ paths,
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

    val orgWithPaths = addPaths(paths)

    dao
      .insert(orgWithPaths, dao.collection.writeConcern)
      .toSuccess(PlatformServiceError("error inserting organization"))
      .map(id => orgWithPaths.copy(id = id))
  }

  override def findOneById(orgId: ObjectId): Option[Organization] = dao.findOneById(orgId)

  override def findOneByName(name: String): Option[Organization] = dao.findOne(MongoDBObject("name" -> name))

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  override def getTree(parentId: ObjectId) = dao.find(MongoDBObject(Keys.path -> parentId)).toStream

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

  override def list(sk: Int, l: Int): Stream[Organization] = dao.find(MongoDBObject.empty).skip(sk).limit(l).toStream

  override def orgsWithPath(orgId: ObjectId, deep: Boolean): Stream[Organization] = {
    val cursor = if (deep) dao.find(MongoDBObject(Keys.path -> orgId)) else dao.find(MongoDBObject(Keys.id -> orgId)) //find the tree of the given organization
    cursor.toStream
  }

  override def save(o: Organization): Validation[PlatformServiceError, Organization] =
    Validation.fromTryCatch(dao.save(o)).bimap(
      t => PlatformServiceError("Error saving", t),
      _ => o)

}
