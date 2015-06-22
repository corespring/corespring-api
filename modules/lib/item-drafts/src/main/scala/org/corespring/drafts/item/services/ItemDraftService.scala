package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ DBObject, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ DraftId, ItemDraft, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId

object ItemDraftDbUtils {
  import com.novus.salat.grater
  import org.corespring.platform.core.models.mongoContext.context
  import scala.language.implicitConversions

  def idToDbo(draftId: DraftId): DBObject = {
    val id = grater[DraftId].asDBObject(draftId)
    MongoDBObject("_id" -> id)
  }

  implicit def toDbo(dbo: ItemDraft): DBObject = {
    grater[ItemDraft].asDBObject(dbo)
  }

  implicit def toDraft(dbo: DBObject): ItemDraft = {
    grater[ItemDraft].asObject(new MongoDBObject(dbo))
  }

}

trait ItemDraftService {

  import ItemDraftDbUtils._

  private object IdKeys {
    val orgId: String = "_id.orgId"
    val itemId: String = "_id.itemId"
  }

  def collection: MongoCollection

  import com.novus.salat.grater
  import org.corespring.platform.core.models.mongoContext.context

  import scala.language.implicitConversions

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
    val query = MongoDBObject(IdKeys.itemId -> itemId)
    val result = collection.remove(query)
    result.getLastError.ok
  }

  def remove(d: ItemDraft): Boolean = remove(d.id)

  def listByOrgAndVid(orgId: ObjectId, vid: VersionedId[ObjectId]) = {
    val query = MongoDBObject(IdKeys.orgId -> orgId, IdKeys.itemId -> vid.id)
    collection.find(query).map(toDraft)
  }

  def listForOrg(orgId: ObjectId) = collection.find(MongoDBObject(IdKeys.orgId -> orgId)).toSeq.map(toDraft)

  def listByItemAndOrgId(itemId: VersionedId[ObjectId], orgId: ObjectId) = {
    val query = MongoDBObject(IdKeys.orgId -> orgId, IdKeys.itemId -> itemId.id)
    collection.find(query).map(toDraft)
  }

}
