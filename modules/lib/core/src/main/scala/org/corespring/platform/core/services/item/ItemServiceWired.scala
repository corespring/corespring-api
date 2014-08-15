package org.corespring.platform.core.services.item

import com.mongodb.casbah
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{ Imports, MongoDBObject }
import com.novus.salat._
import com.novus.salat.dao.SalatMongoCursor
import org.bson.types.ObjectId
import org.corespring.assets.CorespringS3Service
import org.corespring.assets.CorespringS3ServiceExtended
import org.corespring.common.config.AppConfig
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.files.CloneFileResult
import org.corespring.platform.core.files.ItemFiles
import org.corespring.platform.core.models.item.resource.BaseFile.ContentTypes
import org.corespring.platform.core.models.item.resource.{ CDataHandler, VirtualFile, Resource }
import org.corespring.platform.core.models.item.{ Item, FieldValue }
import org.corespring.platform.core.models.itemSession.{ ItemSessionCompanion, DefaultItemSession }
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.Application
import play.api.PlayException
import scala.Some
import scala.xml.Elem
import scalaz._
import se.radley.plugin.salat.SalatPlugin

class ItemServiceWired(
  val s3service: CorespringS3Service,
  sessionCompanion: ItemSessionCompanion,
  val dao: SalatVersioningDao[Item])
  extends ItemService with PackageLogging with ItemFiles {

  import com.mongodb.casbah.commons.conversions.scala._
  import org.corespring.platform.core.models.mongoContext.context

  RegisterJodaTimeConversionHelpers()
  val FieldValuesVersion = "0.0.1"

  lazy val collection = dao.currentCollection

  lazy val fieldValues = FieldValue.current

  private val baseQuery = MongoDBObject("contentType" -> "item")

  def clone(item: Item): Option[Item] = {
    val itemClone = item.cloneItem
    val result: Validation[Seq[CloneFileResult], Item] = cloneStoredFiles(itemClone)
    result match {
      case Success(updatedItem) => {
        dao.save(updatedItem, false)
        Some(updatedItem)
      }
      case Failure(files) => {
        None
      }
    }
  }

  def count(query: DBObject, fields: Option[String] = None): Int = dao.countCurrent(baseQuery ++ query).toInt

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[Item] = dao.findCurrent(baseQuery ++ query, fields)

  def findOneById(id: VersionedId[ObjectId]): Option[Item] = dao.findOneById(id)

  def findOne(query: DBObject): Option[Item] = dao.findOneCurrent(baseQuery ++ query)

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false) = dao.update(id, dbo, createNewVersion)

  def deleteUsingDao(id: VersionedId[ObjectId]) = dao.delete(id)

  // three things occur here: 1. save the new item, 2. copy the old item's s3 files, 3. update the old item's stored files with the new s3 locations
  // TODO if any of these three things fail, the database and s3 revert back to previous state
  def save(item: Item, createNewVersion: Boolean = false) = {

    dao.save(item.copy(dateModified = Some(new DateTime())), createNewVersion)

    if (createNewVersion) {

      val newItem = dao.findOneById(VersionedId(item.id.id)).get
      val result: Validation[Seq[CloneFileResult], Item] = cloneStoredFiles(newItem)
      result match {
        case Success(updatedItem) => {
          dao.save(updatedItem, false)
        }
        case Failure(files) => {
          dao.revertToVersion(item.id)
          files.foreach(r => if (r.successful) { s3service.delete(bucket, r.file.storageKey) })
        }
      }
    }
  }

  def insert(i: Item): Option[VersionedId[ObjectId]] = dao.insert(i)

  def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[Item] = {
    val oids = ids.map(i => i.id)
    val query = baseQuery ++ MongoDBObject("_id._id" -> MongoDBObject("$in" -> oids))
    val out = dao.findCurrent(query, keys).toSeq
    out
  }

  def getQtiXml(id: VersionedId[ObjectId]): Option[Elem] = {
    import com.mongodb.casbah.commons.Implicits._

    def withQti(dbo: DBObject): Option[DBObject] = {
      val obj = dbo.get(Item.Keys.data)
      if (obj == null) {
        logger.warn(s"The item $id - has no QTI! - if this is a v1 item this could be a problem")
        None
      } else {
        Some(dbo)
      }
    }

    for {
      dbo <- findFieldsById(id)
      dboWithQti <- withQti(dbo)
      resource <- Some(grater[Resource].asObject(dbo.get(Item.Keys.data).asInstanceOf[DBObject]))
      mainFile <- resource.files.find(f => f.isMain && f.contentType == ContentTypes.XML)
      virtualFile: VirtualFile <- if (mainFile.isInstanceOf[VirtualFile]) Some(mainFile.asInstanceOf[VirtualFile]) else None
    } yield scala.xml.XML.loadString(CDataHandler.addCDataTags(virtualFile.content))
  }

  def sessionCount(item: Item): Long = {
    import com.novus.salat._
    val dbo = grater[VersionedId[ObjectId]].asDBObject(item.id)
    val query = MongoDBObject("itemId" -> dbo)
    sessionCompanion.count(query) + v2SessionCount(item.id)
  }

  def v2SessionCount(itemId:VersionedId[ObjectId]) : Long = ItemVersioningDao.db("v2.itemSessions").count(MongoDBObject("itemId" -> itemId.toString))

  def bucket: String = AppConfig.assetsBucket

}

object ItemVersioningDao extends SalatVersioningDao[Item] {

  import play.api.Play.current

  private def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  def db: casbah.MongoDB = salatDb()

  protected def collectionName: String = "content"

  protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

  protected implicit def context: Context = org.corespring.platform.core.models.mongoContext.context

}

object ItemServiceWired extends ItemServiceWired(
  CorespringS3ServiceExtended,
  DefaultItemSession,
  ItemVersioningDao
)

