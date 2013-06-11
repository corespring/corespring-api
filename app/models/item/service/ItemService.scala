package models.item.service
import com.mongodb.{BasicDBObject, DBObject}
import com.novus.salat.dao.SalatMongoCursor
import models.item.Item
import org.bson.types.ObjectId
import scala.xml.Elem

trait ItemServiceClient{
  def itemService : ItemService
}

trait ItemService {

  def cloneItem(item: Item): Option[Item]

  def findOneByIdAndVersion(id: ObjectId, version: Option[Int]): Option[Item]

  def findFieldsByIdAndVersion(id:ObjectId, version:Option[Int], fields : DBObject) : Option[DBObject]

  def currentVersion(id:ObjectId) : Option[Int]

  def countItems(query: DBObject, fields: Option[String] = None): Int

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[Item]

  def findOneById(id: ObjectId): Option[Item]

  def findOne(query: DBObject): Option[Item]

  def save(i: Item, createNewVersion: Boolean = false)

  /** Save using a dbo - allows finer grained updates using $set */
  def saveUsingDbo(id:ObjectId, dbo: DBObject, createNewVersion: Boolean = false)

  def insert(i: Item): Option[ObjectId]

  def findMultiple(ids: Seq[ObjectId], keys: DBObject): Seq[Item]

  def getQtiXml(id: ObjectId, version: Option[Int] = None): Option[Elem]

}
