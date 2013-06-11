package models.item.service

import com.mongodb.casbah
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{BasicDBObject, DBObject}
import com.novus.salat._
import com.novus.salat.dao.SalatMongoCursor
import models.item.resource.BaseFile.ContentTypes
import models.item.resource.{VirtualFile, Resource}
import models.item.{FieldValue, Item}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.joda.time.DateTime
import play.api.Application
import play.api.PlayException
import scala.xml.Elem
import se.radley.plugin.salat.SalatPlugin

object ItemServiceImpl extends ItemService {

  import models.mongoContext.context

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  lazy val dao = new SalatVersioningDao[Item] {

    import play.api.Play.current

    protected def db: casbah.MongoDB = salatDb()

    protected def collectionName: String = "content"

    protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

    protected implicit def context: Context = models.mongoContext.context
  }

  import com.mongodb.casbah.commons.conversions.scala._

  RegisterJodaTimeConversionHelpers()
  val FieldValuesVersion = "0.0.1"

  lazy val collection = dao.currentCollection


  lazy val fieldValues = FieldValue.current


  def cloneItem(item: Item): Option[Item] = {
    val clonedItem = item.cloneItem
    dao.save(clonedItem)
    Some(clonedItem)
  }

  def findOneByIdAndVersion(id: ObjectId, version: Option[Int]): Option[Item] = version.map(dao.get(id, _)).getOrElse(dao.get(id))

  def countItems(query: DBObject, fields: Option[String] = None): Int = dao.count(query).toInt

  def findFieldsByIdAndVersion(id: ObjectId, version: Option[Int], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, version, fields)

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[Item] = dao.find(query, fields)

  def findOneById(id: ObjectId): Option[Item] = dao.findOneById(id)

  def findOne(query: DBObject): Option[Item] = dao.findOne(query)

  def saveUsingDbo(id:ObjectId, dbo:DBObject, createNewVersion : Boolean = false) = dao.update(id, dbo, createNewVersion)

  def save(i: Item, createNewVersion: Boolean = false) = dao.save(i.copy(dateModified = Some(new DateTime())), createNewVersion)

  def insert(i: Item): Option[ObjectId] = dao.insert(i)

  def findMultiple(ids: Seq[ObjectId], keys: DBObject): Seq[Item] = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    dao.find(query, keys).toSeq
  }

  def getQtiXml(id: ObjectId, version: Option[Int]): Option[Elem] = {
    import com.mongodb.casbah.commons.Implicits._
    for {
      dbo <- findFieldsByIdAndVersion(id, version)
      resource <- Some(grater[Resource].asObject(dbo.get(Item.Keys.data).asInstanceOf[DBObject]))
      mainFile <- resource.files.find(f => f.isMain && f.contentType == ContentTypes.XML)
      virtualFile: VirtualFile <- if (mainFile.isInstanceOf[VirtualFile]) Some(mainFile.asInstanceOf[VirtualFile]) else None
    } yield scala.xml.XML.loadString(virtualFile.content)
  }


  def currentVersion(id: ObjectId): Option[Int] = dao.getCurrentVersion(id)
}



