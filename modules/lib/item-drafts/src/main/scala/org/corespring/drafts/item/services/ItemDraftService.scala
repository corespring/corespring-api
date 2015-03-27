package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ DBObject, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ ItemDraft, OrgAndUser }

trait ItemDraftService {

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

  def load(id: ObjectId): Option[ItemDraft] = {
    collection.findOneByID(id).map(dbo => {
      toDraft(dbo)
    })
  }

  def owns(user: OrgAndUser, id: ObjectId) = collection.count(MongoDBObject("_id" -> id, "user.org.id" -> user.org.id)) == 1

  def remove(d: ItemDraft): Boolean = {
    val result = collection.remove(MongoDBObject("_id" -> d.id))
    result.getN == 1
  }

  def findByIdAndVersion(id: ObjectId, version: Long): Seq[ItemDraft] = {
    collection
      .find(MongoDBObject("src._id._id" -> id, "src._id.version" -> version))
      .toSeq
      .map(toDraft)
  }

  def listForOrg(orgId: ObjectId) = collection.find(MongoDBObject("user.orgId" -> orgId)).toSeq.map(toDraft)

  def removeDraftByIdAndUser(id: ObjectId, user: OrgAndUser) = {
    collection.remove(MongoDBObject("_id" -> id, "user.org.id" -> user.org.id))
  }
}
