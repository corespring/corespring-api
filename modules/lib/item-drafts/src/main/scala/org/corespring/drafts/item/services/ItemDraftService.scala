package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ DBObject, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ DraftId, ItemDraft, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId

trait ItemDraftService {

  protected val userOrgId: String = "_id.user.org._id"

  def collection: MongoCollection

  import com.novus.salat.grater
  import org.corespring.platform.core.models.mongoContext.context

  import scala.language.implicitConversions

  private implicit def toDbo(dbo: ItemDraft): DBObject = {
    grater[ItemDraft].asDBObject(dbo)
  }

  private implicit def toDraft(dbo: DBObject): ItemDraft = {
    grater[ItemDraft].asObject(new MongoDBObject(dbo))
  }

  def save(d: ItemDraft): WriteResult = {
    collection.save(d)
  }

  private def idToDbo(draftId: DraftId): DBObject = {
    val id = grater[DraftId].asDBObject(draftId)
    MongoDBObject("_id" -> id)
  }

  def load(id: DraftId): Option[ItemDraft] = {
    collection.findOne(idToDbo(id)).map(dbo => {
      toDraft(dbo)
    })
  }

  def owns(ou: OrgAndUser, id: DraftId) = {
    val orgMatches = ou.org.id == id.orgId
    val userMatches = ou.user.map(_.userName == id.name)
    orgMatches && userMatches.getOrElse(true)
  }

  def remove(id: DraftId): Boolean = {
    val result = collection.remove(idToDbo(id))
    result.getN == 1
  }

  def removeByItemId(itemId: ObjectId): Boolean = {
    val query = MongoDBObject("_id.itemId" -> itemId)
    val result = collection.remove(query)
    result.getLastError.ok
  }

  def remove(d: ItemDraft): Boolean = remove(d.id)

  def listByOrgAndVid(orgId: ObjectId, vid: VersionedId[ObjectId]) = {
    val query = MongoDBObject("_id.orgId" -> orgId, "_id.itemId" -> vid.id)
    collection.find(query).map(toDraft)
  }

  def listForOrg(orgId: ObjectId) = collection.find(MongoDBObject(userOrgId -> orgId)).toSeq.map(toDraft)

  def listByItemAndOrgId(itemId: VersionedId[ObjectId], orgId: ObjectId) = {
    val query = MongoDBObject("_id.orgId" -> orgId, "_id.itemId" -> itemId.id)
    collection.find(query).map(toDraft)
  }

  def removeNonConflictingDraftsForOrg(itemId: ObjectId, orgId: ObjectId): Seq[DraftId] = {
    val query = MongoDBObject("_id" -> MongoDBObject("itemId" -> itemId, "user.org._id" -> orgId, "hasConflict" -> false))

    val ids = collection.find(query, MongoDBObject()).toSeq.map { dbo =>
      val id = dbo.get("_id").asInstanceOf[DBObject]
      grater[DraftId].asObject(new MongoDBObject(id))
    }

    val result = collection.remove(query)
    if (ids.length == result.getN) {
      ids
    } else {
      throw new RuntimeException("Error deleting all items")
    }
  }

}
