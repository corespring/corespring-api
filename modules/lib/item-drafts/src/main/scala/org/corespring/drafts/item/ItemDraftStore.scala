package org.corespring.drafts.item

import com.mongodb.{ BasicDBObject, DBObject }
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.drafts.DraftStore
import org.corespring.drafts.errors.{ DraftError, SaveDataFailed }
import org.corespring.drafts.models.{ Draft, CommittedDraft, DraftSrc, UserDraft }
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

trait DraftService[ID, DRAFT] {

  def save(draft: DRAFT): Either[String, Unit]

  def load(id: ID): Option[DRAFT]
}

case class SimpleUser(id: ObjectId, userName: String, provider: String, fullName: String)

object SimpleUser {
  def apply(u: User): SimpleUser = {
    SimpleUser(u.id, u.userName, u.provider, u.fullName)
  }
}

case class ItemUserDraft(
  val id: ObjectId,
  override val data: Item,
  override val user: SimpleUser,
  override val draftSrc: DraftSrc[ObjectId, Long]) extends UserDraft[Item, SimpleUser, ObjectId, Long] {
  override def update(d: Item): ItemUserDraft = this.copy(data = d)
}

import org.corespring.platform.core.models.mongoContext.context

trait ItemDraftService extends DraftService[ObjectId, ItemUserDraft] {

  type UD = UserDraft[Item, SimpleUser, ObjectId, Long]

  def collection: MongoCollection

  def toDbo(d: ItemUserDraft): DBObject = {
    MongoDBObject(
      "_id" -> d.id,
      "data" -> com.novus.salat.grater[Item].asDBObject(d.data),
      "user" -> com.novus.salat.grater[SimpleUser].asDBObject(d.user),
      "src" -> MongoDBObject(
        "id" -> d.data.id.id,
        "version" -> d.data.id.version.get))
  }

  implicit def bToM(b: BasicDBObject): MongoDBObject = new MongoDBObject(b)

  def toDraft(dbo: MongoDBObject): ItemUserDraft = {

    val id = dbo.getAs[ObjectId]("_id").get
    val item = dbo.getAs[BasicDBObject]("data").map { i => com.novus.salat.grater[Item].asObject(i) }.get
    val user = dbo.getAs[BasicDBObject]("user").map { u => com.novus.salat.grater[SimpleUser].asObject(u) }.get

    def toSrc(dbo: MongoDBObject) = {
      DraftSrc[ObjectId, Long](dbo.getAs[ObjectId]("id").get, dbo.getAs[Long]("version").get)
    }

    val src = toSrc(dbo.getAs[BasicDBObject]("src").get)
    ItemUserDraft(id, item, user, src)
  }

  override def save(data: ItemUserDraft): Either[String, Unit] = {
    val dbo: DBObject = toDbo(data)
    collection.save(dbo)
    Right()
  }

  override def load(id: ObjectId): Option[ItemUserDraft] = {
    collection.findOneByID(id).map { dbo =>
      toDraft(new MongoDBObject(dbo))
    }
  }
}

trait CommitService {
  def saveCommit(commit: CommittedDraft[SimpleUser, ObjectId, Long]): Either[String, CommittedDraft[SimpleUser, ObjectId, Long]]

  def findCommits(id: ObjectId): Seq[CommittedDraft[SimpleUser, ObjectId, Long]]
}

trait ItemCommitService extends CommitService {

  def collection: MongoCollection

  override def saveCommit(commit: CommittedDraft[SimpleUser, ObjectId, Long]): Either[String, CommittedDraft[SimpleUser, ObjectId, Long]] = {
    val dbo = com.novus.salat.grater[CommittedDraft[SimpleUser, ObjectId, Long]].asDBObject(commit)
    val result = collection.save(dbo)
    if (result.getLastError.ok) {
      Right(commit)
    } else {
      Left(result.getLastError.getErrorMessage)
    }
  }

  override def findCommits(id: ObjectId): Seq[CommittedDraft[SimpleUser, ObjectId, Long]] = {
    collection.find(MongoDBObject("dataId" -> id)).map[CommittedDraft[SimpleUser, ObjectId, Long]] { dbo =>
      com.novus.salat.grater[CommittedDraft[SimpleUser, ObjectId, Long]].asObject(new MongoDBObject(dbo))
    }.toSeq
  }
}

trait ItemDraftStore extends DraftStore[SimpleUser, ObjectId, Long, Item, ItemUserDraft] {

  def itemService: ItemService

  def commitService: CommitService

  override def loadDataAndVersion(id: ObjectId): (Item, Long) = {
    itemService.findOneById(VersionedId(id, None)).map { i =>
      (i -> i.id.version.get)
    }.getOrElse {
      throw new RuntimeException("Can't find item")
    }
  }

  override def saveData(id: ObjectId,
    user: SimpleUser,
    data: Item,
    src: DraftSrc[ObjectId, Long]): Either[DraftError, CommittedDraft[SimpleUser, ObjectId, Long]] = {

    //save this item as a new version
    itemService.save(data, true)
    //save the commit info
    val commit = CommittedDraft[SimpleUser, ObjectId, Long](id, user, src, new DateTime())
    commitService.saveCommit(commit) match {
      case Left(err) => Left(SaveDataFailed(err))
      case Right(commit) => Right(commit)
    }
  }

  override def loadCommittedDrafts(id: ObjectId): Seq[CommittedDraft[SimpleUser, ObjectId, Long]] = {
    commitService.findCommits(id)
  }

  override def mkUserDraft(data: Item, user: SimpleUser, src: DraftSrc[ObjectId, Long]): ItemUserDraft = {
    ItemUserDraft(ObjectId.get, data, user, src)
  }
}