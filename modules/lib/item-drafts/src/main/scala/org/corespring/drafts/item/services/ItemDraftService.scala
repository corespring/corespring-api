package org.corespring.drafts.item.services

import com.mongodb.casbah.Imports._
import org.corespring.drafts.item.models.{ ItemDraftHeader, DraftId, ItemDraft, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.Logger

object ItemDraftConfig {
  object CollectionNames {
    val itemDrafts = "drafts.items"
    val commits = "drafts.item_commits"
  }
}

private[drafts] trait ItemDraftDbUtils {
  implicit def context: com.novus.salat.Context
  import com.novus.salat.grater
  import scala.language.implicitConversions

  protected def idToDbo(draftId: DraftId): DBObject = {
    val id = grater[DraftId].asDBObject(draftId)
    MongoDBObject("_id" -> id)
  }

  protected implicit def toDbo(dbo: ItemDraft): DBObject = {
    grater[ItemDraft].asDBObject(dbo)
  }

  protected implicit def toDraft(dbo: DBObject): ItemDraft = {
    grater[ItemDraft].asObject(new MongoDBObject(dbo))
  }
}

trait ItemDraftService extends ItemDraftDbUtils {

  val logger = Logger(classOf[ItemDraftService])

  private object IdKeys {
    val orgId: String = "_id.orgId"
    val itemId: String = "_id.itemId"
  }

  def loadExpired: Boolean

  def collection: MongoCollection

  collection.ensureIndex(IdKeys.orgId)
  collection.ensureIndex(IdKeys.itemId)

  import scala.language.implicitConversions

  /**
   * Add expires condition to query (if enabled), to only load drafts whose expires is in the future.
   * @param q
   * @return
   */
  private def addExpires(q: DBObject): DBObject = {
    q ++ (if (loadExpired) MongoDBObject() else ("expires" $gte DateTime.now()))
  }

  def save(d: ItemDraft): WriteResult = {
    collection.save(d)
  }

  def load(id: DraftId): Option[ItemDraft] = {
    val query = addExpires(idToDbo(id))
    collection.findOne(query).map(toDraft(_))
  }

  def owns(ou: OrgAndUser, id: DraftId) = {
    val orgMatches = ou.org.id == id.orgId
    val userMatches = ou.user.map(_.userName == id.name)
    orgMatches && userMatches.getOrElse(true)
  }

  def remove(id: DraftId): Int = {
    val result = collection.remove(idToDbo(id))
    result.getN
  }

  def remove(d: ItemDraft): Int = remove(d.id)

  def removeByItemId(itemId: ObjectId): Int = {
    val query = MongoDBObject(IdKeys.itemId -> itemId)
    val result = collection.remove(query)
    result.getN
  }

  def listByOrgAndVid(orgId: ObjectId, vid: VersionedId[ObjectId]) = {
    val query = addExpires(MongoDBObject(IdKeys.orgId -> orgId, IdKeys.itemId -> vid.id))
    collection.find(query).map(toDraft)
  }

  private def toHeader(dbo: DBObject): ItemDraftHeader = {
    import com.novus.salat.grater

    val idDbo: BasicDBObject = dbo.expand[BasicDBObject]("_id").getOrElse {
      throw new RuntimeException(s"Not a db object: ${dbo.get("_id")}")
    }

    val id = grater[DraftId].asObject(idDbo)
    val created = dbo.get("created").asInstanceOf[DateTime]
    val expires = dbo.get("expires").asInstanceOf[DateTime]
    val userName = dbo.expand[String]("user.user.userName")
    ItemDraftHeader(id, created, expires, userName)
  }

  def listForOrg(orgId: ObjectId, limit: Int = 0, skip: Int = 0): Seq[ItemDraftHeader] = {
    val query = addExpires(MongoDBObject(IdKeys.orgId -> orgId))
    val fields = MongoDBObject("created" -> 1, "expires" -> 1, "user.user.userName" -> 1)
    val dbos = collection.find(query, fields).skip(0).limit(0)
    dbos.map(toHeader).toSeq
  }

  def listByItemAndOrgId(itemId: VersionedId[ObjectId], orgId: ObjectId): Seq[ItemDraftHeader] = {
    val query = addExpires(MongoDBObject(IdKeys.orgId -> orgId, IdKeys.itemId -> itemId.id))
    val fields = MongoDBObject("created" -> 1, "expires" -> 1, "user.user.userName" -> 1)
    logger.trace(s"function=listByItemAndOrgId, collection=${collection.name}, query=$query, fields=$fields")
    collection.find(query, fields).map(toHeader).toSeq
  }

}
