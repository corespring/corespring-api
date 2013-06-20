package models.item.service

import com.mongodb.casbah
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{BasicDBObject, DBObject}
import com.novus.salat._
import com.novus.salat.dao.SalatMongoCursor
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

class ItemServiceImpl(s3service: S3Service) extends ItemService with PackageLogging{
  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

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

    private def iterateStoredFiles(item:Item, versionS3File:(StoredFile) => String):Validation[SalatVersioningDaoException,Unit] = {
      //for each stored file in item, copy the file to the version path
      try {
        item.data.get.files.foreach {
          file => file match {
            case sf: StoredFile =>
              val newKey = versionS3File(sf)
              sf.storageKey = newKey
            case _ =>
          }
        }
        item.supportingMaterials.foreach {
          sm =>
            sm.files.filter(_.isInstanceOf[StoredFile]).foreach {
              file =>
                val sf = file.asInstanceOf[StoredFile]
                val newKey = versionS3File(sf)
                sf.storageKey = newKey
            }
        }
        Success(())
      } catch {
        case r: RuntimeException =>
          Logger.error("Error cloning some of the S3 files: " + r.getMessage)
          Logger.error(r.getStackTrace.mkString("\n"))
          Failure(SalatVersioningDaoException("Error cloning some of the S3 files: " + r.getMessage))
      }
    }
    def cloneEntity(item:Item) = item.cloneItem
    override protected def beforeClone(item:Item, itemClone:Item):Validation[SalatVersioningDaoException,Unit] = {
      def versionS3File(sourceFile: StoredFile): String = {
        val oldStorageKeyIdRemoved = sourceFile.storageKey.replaceAll("^[0-9a-fA-F]+/", "")
        val oldStorageKeyVersionRemoved = oldStorageKeyIdRemoved.replaceAll("^\\d+?/","")
        val newStorageKey = itemClone.id.toString +"/"+ oldStorageKeyVersionRemoved
        s3service.cloneFile(AMAZON_ASSETS_BUCKET, sourceFile.storageKey, newStorageKey)
        newStorageKey
      }
      iterateStoredFiles(item,versionS3File)
    }
    override protected def beforeVersionedInsert(item:Item, version:Int):Validation[SalatVersioningDaoException,Unit] = {
      def versionS3File(sourceFile: StoredFile): String = {
        val oldStorageKeyIdRemoved = sourceFile.storageKey.replaceAll("^[0-9a-fA-F]+/", "")
        val oldStorageKeyVersionRemoved = oldStorageKeyIdRemoved.replaceAll("^\\d+?/","")
        val newStorageKey = item.id.toString +"/"+ version +"/"+ oldStorageKeyVersionRemoved
        s3service.cloneFile(AMAZON_ASSETS_BUCKET, sourceFile.storageKey, newStorageKey)
        newStorageKey
      }
      iterateStoredFiles(item,versionS3File)
    }
  }


  import com.mongodb.casbah.commons.conversions.scala._

  RegisterJodaTimeConversionHelpers()
  val FieldValuesVersion = "0.0.1"

  lazy val collection = dao.currentCollection


  lazy val fieldValues = FieldValue.current

  def cloneItem(id:ObjectId): Option[Item] = dao.clone(id).toOption

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
object ItemServiceImpl extends ItemServiceImpl(ConcreteS3Service)



