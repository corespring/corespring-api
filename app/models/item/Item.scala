package models.item


import com.mongodb.casbah.Imports._
import controllers.JsonValidationException
import models.itemSession.DefaultItemSession
import models.json.ItemView
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.{EntityWithVersionedId, VersionedId}
import org.joda.time.DateTime
import play.api.libs.json._
import resource.Resource
import scala._
import models.mongoContext._
import com.novus.salat._
import com.novus.salat.global._

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
                 var id: VersionedId[ObjectId] = VersionedId(ObjectId.get())) extends Content with EntityWithVersionedId[ObjectId] {
  def sessionCount: Int = DefaultItemSession.find(MongoDBObject("itemId" -> grater[VersionedId[ObjectId]].asDBObject(id))).count

  def cloneItem: Item = {
    val taskInfoCopy = taskInfo.getOrElse(TaskInfo(title = Some(""))).cloneInfo("[copy]")
    copy(id = VersionedId(ObjectId.get()), taskInfo = Some(taskInfoCopy), published = false)
  }
}


object Item {

  object Dbo {

    //TODO: versioning-dao - this was used in master in Item - needs to be reintroduced (compare with master and find the usages).
    def asMetadataOnly(i: Item): DBObject = {
      import Item.Keys._
      import com.mongodb.casbah.commons.MongoDBObject
      import com.novus.salat._
      import models.mongoContext.context
      val timestamped = i.copy(dateModified = Some(new DateTime()))
      val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(timestamped))
      dbo - "_id" - supportingMaterials - data - collectionId
    }
  }

  val FieldValuesVersion = "0.0.1"

  object Keys {

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
  }


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

    import Keys._

    def reads(json: JsValue): Item = {
      val item = Item()

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
        import models.versioning.VersionedIdImplicits.Reads
        item.id = (json \ id).asOpt[VersionedId[ObjectId]].getOrElse(VersionedId(new ObjectId()))
      } catch {
        case e: Throwable => throw new JsonValidationException(id)
      }
      item
    }
  }

}
