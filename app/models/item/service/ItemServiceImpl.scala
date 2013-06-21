package models.item.service

import com.mongodb.casbah
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{BasicDBObject, DBObject}
import com.novus.salat._
import dao.{SalatInsertError, SalatMongoCursor}
import models.item.resource.BaseFile.ContentTypes
import models.item.resource.{StoredFile, VirtualFile, Resource}
import models.item.{FieldValue, Item}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.joda.time.DateTime
import play.api.Application
import play.api.PlayException
import scala.xml.Elem
import se.radley.plugin.salat.SalatPlugin
import common.log.PackageLogging
import controllers.{ConcreteS3Service, S3Service}
import com.typesafe.config.ConfigFactory
import scalaz.Scalaz._
import scalaz._
import org.corespring.platform.data.mongo.exceptions.SalatVersioningDaoException
import api.v1.ResourceApi
import org.corespring.platform.data.mongo.models.VersionedId
import com.sun.xml.internal.bind.v2.TODO
import web.controllers.utils.ConfigLoader

class ItemServiceImpl(s3service: S3Service) extends ItemService with PackageLogging{

  private val AMAZON_ASSETS_BUCKET = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

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

  def cloneItem(item:Item): Option[Item] = {
    val itemClone = item.cloneItem
    def versionS3File(sourceFile: StoredFile): String = {
      val oldStorageKeyIdRemoved = sourceFile.storageKey.replaceAll("^[0-9a-fA-F]+/", "")
      val oldStorageKeyVersionRemoved = oldStorageKeyIdRemoved.replaceAll("^\\d+?/","")
      val newStorageKey = itemClone.id.toString +"/"+ oldStorageKeyVersionRemoved
      s3service.cloneFile(AMAZON_ASSETS_BUCKET, sourceFile.storageKey, newStorageKey)
      newStorageKey
    }
    try {
      itemClone.data.get.files.foreach {
        file => file match {
          case sf: StoredFile =>
            val newKey = versionS3File(sf)
            sf.storageKey = newKey
          case _ =>
        }
      }
      itemClone.supportingMaterials.foreach {
        sm =>
          sm.files.filter(_.isInstanceOf[StoredFile]).foreach {
            file =>
              val sf = file.asInstanceOf[StoredFile]
              val newKey = versionS3File(sf)
              sf.storageKey = newKey
          }
      }
      dao.insert(itemClone).flatMap(findOneById(_))
    } catch {
      case e:SalatInsertError =>
        Logger.error("Error inserting item clone: "+e.getMessage)
        None
      case r: RuntimeException =>
        Logger.error("Error cloning some of the S3 files: " + r.getMessage)
        Logger.error(r.getStackTrace.mkString("\n"))
        None
    }
  }

  def countItems(query: DBObject, fields: Option[String] = None): Int = dao.count(query).toInt

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[Item] = dao.find(query, fields)

  def findOneById(id: VersionedId[ObjectId]): Option[Item] = dao.get(id)

  def findOne(query: DBObject): Option[Item] = dao.findOne(query)

  def saveUsingDbo(id:VersionedId[ObjectId], dbo:DBObject, createNewVersion : Boolean = false) = dao.update(id, dbo, createNewVersion)

  def save(i: Item, createNewVersion: Boolean = false) = dao.save(i.copy(dateModified = Some(new DateTime())), createNewVersion)

  def insert(i: Item): Option[VersionedId[ObjectId]] = dao.insert(i)

  def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[Item] = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    dao.find(query, keys).toSeq
  }

  def getQtiXml(id: VersionedId[ObjectId]): Option[Elem] = {
    import com.mongodb.casbah.commons.Implicits._
    for {
      dbo <- findFieldsById(id)
      resource <- Some(grater[Resource].asObject(dbo.get(Item.Keys.data).asInstanceOf[DBObject]))
      mainFile <- resource.files.find(f => f.isMain && f.contentType == ContentTypes.XML)
      virtualFile: VirtualFile <- if (mainFile.isInstanceOf[VirtualFile]) Some(mainFile.asInstanceOf[VirtualFile]) else None
    } yield scala.xml.XML.loadString(virtualFile.content)
  }


  def currentVersion(id: VersionedId[ObjectId]): Option[Int] = throw new RuntimeException("to be implemented?")
}
object ItemServiceImpl extends ItemServiceImpl(ConcreteS3Service)



