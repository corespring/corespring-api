package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ DBObject, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ DraftId, ItemDraft, OrgAndUser }

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

  def hasConflict(id: DraftId): Option[Boolean] = load(id).map { d => d.hasConflict }

  def owns(user: OrgAndUser, id: DraftId) = user.user.exists(_.userName == id.name) && id.orgId == user.org.id

  def remove(d: ItemDraft): Boolean = {
    val result = collection.remove(MongoDBObject("_id" -> idToDbo(d.id)))
    result.getN == 1
  }

  def listForOrg(orgId: ObjectId) = collection.find(MongoDBObject(userOrgId -> orgId)).toSeq.map(toDraft)

  def removeNonConflictingDraftsForOrg(itemId: ObjectId, orgId: ObjectId): Seq[DraftId] = {
    val query = MongoDBObject("_id" -> MongoDBObject("itemId" -> itemId, "user.org._id" -> orgId, "hasConflict" -> false))

    val ids = collection.find(query, MongoDBObject()).toSeq.map { dbo =>
      val id = dbo.get("_id")
      grater[DraftId].asObject(id)
    }

    val result = collection.remove(query)
    if (ids.length == result.getN) {
      ids
    } else {
      throw new RuntimeException("Error deleting all items")
    }
  }

}
