package models.item


import com.mongodb.casbah
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.TypeImports.DBObject
import com.novus.salat._
import controllers.InternalError
import controllers.JsonValidationException
import dao.{SalatDAOUpdateError, SalatMongoCursor}
import models.itemSession.DefaultItemSession
import models.json.ItemView
import models.mongoContext._
import org.bson.types
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.{Id, VersionedId}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.{PlayException, Application}
import resource.{VirtualFile, BaseFile, Resource}
import scala._
import se.radley.plugin.salat.SalatPlugin


case class Item(
                 var collectionId: String = "",
                 var contributorDetails: Option[ContributorDetails] = None,
                 var contentType: String = "item",
                 var priorUse: Option[String] = None,
                 var priorGradeLevel: Seq[String] = Seq(),
                 var reviewsPassed: Seq[String] = Seq(),
                 var standards: Seq[String] = Seq(),
                 var pValue: Option[String] = None,
                 var lexile: Option[String] = None,
                 var data: Option[Resource] = None,
                 var originId: Option[String] = None,
                 var supportingMaterials: Seq[Resource] = Seq(),
                 var published: Boolean = false,
                 var workflow: Option[Workflow] = None,
                 var dateModified: Option[DateTime] = Some(new DateTime()),
                 var taskInfo: Option[TaskInfo] = None,
                 var otherAlignments: Option[Alignments] = None,
                 var id: ObjectId = new ObjectId()) extends Content with Id[ObjectId] {
  def sessionCount: Int = DefaultItemSession.find(MongoDBObject("itemId" -> id)).count

  def cloneItem: Item = {
    val taskInfoCopy = taskInfo.getOrElse(TaskInfo(title = Some(""))).cloneInfo("[Copy]")
    copy(id = new ObjectId(), taskInfo = Some(taskInfoCopy), published = false)
  }
}


/**
 * An Item model
 */
object Item {

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  lazy val dao = new SalatVersioningDao[VersionedItem, Item] {

    import play.api.Play.current

    protected def db: casbah.MongoDB = salatDb()

    protected def collectionName: String = "content"

    protected def build(id: types.ObjectId, v: Int, entity: Item): VersionedItem =
      VersionedItem(VersionedId(id, v), entity)

    protected implicit def holderManifest: Manifest[VersionedItem] = Manifest.classType(classOf[VersionedItem])

    protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

    protected implicit def context: Context = models.mongoContext.context
  }

  import com.mongodb.casbah.commons.conversions.scala._

  RegisterJodaTimeConversionHelpers()
  val FieldValuesVersion = "0.0.1"

  lazy val collection = dao.currentCollection


  val id = "id"
  val originId = "originId"
  val author = "author"
  val collectionId = Content.collectionId
  val contentType = Content.contentType
  val contributorDetails = "contributorDetails"
  val contributor = "contributor"
  val copyright = "copyright"
  val copyrightOwner = "copyrightOwner"
  val copyrightImageName = "copyrightImageName"
  val copyrightYear = "copyrightYear"
  val copyrightExpirationDate = "copyrightExpirationDate"
  val costForResource = "costForResource"
  val credentials = "credentials"
  val files = "files"
  val gradeLevel = "gradeLevel"
  val priorGradeLevel = "priorGradeLevel"
  val relatedCurriculum = "relatedCurriculum"
  val itemType = "itemType"
  val keySkills = "keySkills"
  val licenseType = "licenseType"
  val subjects = "subjects"
  val primarySubject = "primarySubject"
  val relatedSubject = "relatedSubject"
  val taskInfo = "taskInfo"
  val pValue = "pValue"
  val priorUse = "priorUse"
  val reviewsPassed = "reviewsPassed"
  val demonstratedKnowledge = "demonstratedKnowledge"
  val sourceUrl = "sourceUrl"
  val standards = "standards"
  val title = "title"
  val lexile = "lexile"
  val data = "data"
  val supportingMaterials = "supportingMaterials"
  val bloomsTaxonomy = "bloomsTaxonomy"
  val workflow = "workflow"
  val dateModified = "dateModified"
  val otherAlignments = "otherAlignments"
  val published = "published"

  lazy val fieldValues = FieldValue.current

  implicit object ItemWrites extends Writes[Item] {
    def writes(item: Item) = {
      ItemView.ItemViewWrites.writes(ItemView(item, None))
    }
  }

  def getValidatedValue(s: Seq[KeyValue])(json: JsValue, key: String) = {
    (json \ key).asOpt[String]
      .map(v => if (s.exists(_.key == v)) v else throw new JsonValidationException(key))
  }

  implicit object Reads extends Reads[Item] {
    def reads(json: JsValue): Item = {
      val item = Item()

      /**
       * must do checking outside of json deserialization
       */
      item.collectionId = (json \ collectionId).asOpt[String].getOrElse("")

      item.taskInfo = json.asOpt[TaskInfo]
      item.otherAlignments = json.asOpt[Alignments]
      item.workflow = (json \ workflow).asOpt[Workflow]
      item.contributorDetails = json.asOpt[ContributorDetails]

      item.lexile = (json \ lexile).asOpt[String]
      item.pValue = (json \ pValue).asOpt[String]
      item.originId = (json \ originId).asOpt[String]

      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())

      item.priorUse = (json \ priorUse).asOpt[String]
      item.priorGradeLevel = (json \ priorGradeLevel).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(priorGradeLevel)).getOrElse(Seq.empty)
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.standards = (json \ standards).asOpt[Seq[String]].getOrElse(Seq())
      item.data = (json \ data).asOpt[Resource]
      item.published = (json \ published).asOpt[Boolean].getOrElse(false)

      try {
        item.id = (json \ id).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())
      } catch {
        case e: IllegalArgumentException => throw new JsonValidationException(id)
      }

      item
    }
  }


  def updateItem(oid: ObjectId, newItem: Item, createNewVersion: Boolean = false): Either[InternalError, Item] = {
    try {

      def itemAsDbo: MongoDBObject = {
        val timestamped = newItem.copy(dateModified = Some(new DateTime()))
        val dbo = grater[Item].asDBObject(timestamped)
        dbo - "_id" - supportingMaterials - data - collectionId
      }

      dao.update(oid, MongoDBObject("$set" -> itemAsDbo), createNewVersion = createNewVersion)

      dao.findOneById(oid) match {
        case Some(item) => Right(item)
        case None => Left(InternalError("somehow the document that was just updated could not be found"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("error occured while updating", e))
      case e: IllegalArgumentException => Left(InternalError("destination collection id was not a valid id", e))
      case e: RuntimeException => Left(InternalError(e.getMessage))
    }
  }

  def cloneItem(item: Item): Option[Item] = {
    val clonedItem = item.cloneItem
    dao.save(clonedItem)
    Some(clonedItem)
  }

  def findOneByIdAndVersion(id: ObjectId, version: Option[Int]): Option[Item] = version.map(dao.get(id, _)).getOrElse(dao.get(id))

  def countItems(query: DBObject, fields: Option[String] = None): Int =  dao.count(query).toInt

  def findFieldsById(id: ObjectId, fields: DBObject = MongoDBObject.empty): Option[DBObject] = {
    dao.findDbo(MongoDBObject("_id" -> id), fields).limit(1).toList match {
      case List(dbo) => Some(dbo)
      case _ => None
    }
  }

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[Item] = dao.find(query, fields)

  def findOneById(id: ObjectId): Option[Item] = dao.findOneById(id)

  def findOne(query: DBObject): Option[Item] = dao.findOne(query)

  def save(i: Item, createNewVersion: Boolean = false) = dao.save(i, createNewVersion)

  def insert(i: Item): Option[ObjectId] = dao.insert(i)

  def findMultiple(ids: Seq[ObjectId], keys: DBObject): Seq[Item] = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    dao.find(query, keys).toSeq
  }

  def getQti(itemId: ObjectId): Either[InternalError, String] = {

    findFieldsById(itemId, MongoDBObject(Item.data -> 1)) match {
      case None => Left(InternalError("not found"))
      case Some(o) => o.get(Item.data) match {
        case res: BasicDBObject => {
          grater[Resource].asObject(res).files.find(bf => bf.isMain && bf.contentType == BaseFile.ContentTypes.XML) match {
            case Some(bf) => bf match {
              case vf: VirtualFile => Right(vf.content)
              case _ => Left(InternalError("main file was not a virtual file"))
            }
            case None => Left(InternalError("no main file found that contained xml"))
          }
        }
        case _ => Left(InternalError("data not an object"))
      }
    }
  }

  def findInXml(string: String, collectionIds: List[String]): List[Item] = {

    val query = ".*<assessmentItem.*>.*" + string + ".*<\\/assessmentItem>.*"

    dao.find(
      MongoDBObject(
        "data.files.content" ->
          MongoDBObject(
            "$regex" -> query,
            "$options" -> "ms"),
        "collectionId" -> MongoDBObject("$in" -> collectionIds.toArray)
      ),
      MongoDBObject("taskInfo" -> 1)
    ).toList
  }
}
