package org.corespring.platform.core.models.item

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.json.ContentView
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.core.models.json.{ ItemView, JsonValidationException }
import org.corespring.platform.data.mongo.models.{ EntityWithVersionedId, VersionedId }
import org.joda.time.DateTime
import play.api.libs.json._

case class Item(
  var collectionId: Option[String] = None,
  var contentType: String = Item.contentType,
  var contributorDetails: Option[ContributorDetails] = None,
  var data: Option[Resource] = None,
  var dateModified: Option[DateTime] = Some(new DateTime()),
  var id: VersionedId[ObjectId] = VersionedId(ObjectId.get()),
  var lexile: Option[String] = None,
  var originId: Option[String] = None,
  var otherAlignments: Option[Alignments] = None,
  var pValue: Option[String] = None,
  var playerDefinition: Option[PlayerDefinition] = None,
  var priorGradeLevels: Seq[String] = Seq(),
  var priorUse: Option[String] = None,
  var priorUseOther: Option[String] = None,
  var published: Boolean = false,
  var reviewsPassed: Seq[String] = Seq(),
  var reviewsPassedOther: Option[String] = None,
  var sharedInCollections: Seq[String] = Seq(),
  var standards: Seq[String] = Seq(),
  var supportingMaterials: Seq[Resource] = Seq(),
  var taskInfo: Option[TaskInfo] = None,
  var workflow: Option[Workflow] = None)

  extends Content[VersionedId[ObjectId]] with EntityWithVersionedId[ObjectId] {

  def cloneItem: Item = {
    val taskInfoCopy = taskInfo.getOrElse(TaskInfo(title = Some(""))).cloneInfo("[copy]")
    copy(id = VersionedId(ObjectId.get()), taskInfo = Some(taskInfoCopy), published = false)
  }
}

object Item {

  import org.corespring.platform.core.models.mongoContext.context

  object Dbo {

    //TODO: versioning-dao - this was used in master in Item - needs to be reintroduced (compare with master and find the usages).
    def asMetadataOnly(i: Item): DBObject = {
      import com.mongodb.casbah.commons.MongoDBObject
      import com.novus.salat._
      import org.corespring.platform.core.models.item.Item.Keys._
      val timestamped = i.copy(dateModified = Some(new DateTime()))
      val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(timestamped))
      dbo - "_id" - supportingMaterials - data - collectionId
    }
  }

  val FieldValuesVersion = "0.0.1"

  object QtiResource {
    val QtiXml = "qti.xml"
  }

  val contentType = "item"

  object Keys {

    val author = "author"
    val bloomsTaxonomy = "bloomsTaxonomy"
    val collectionId = Content.collectionId
    val contentType = Content.contentType
    val contributor = "contributor"
    val contributorDetails = "contributorDetails"
    val copyright = "copyright"
    val copyrightExpirationDate = "copyrightExpirationDate"
    val copyrightImageName = "copyrightImageName"
    val copyrightOwner = "copyrightOwner"
    val copyrightYear = "copyrightYear"
    val costForResource = "costForResource"
    val credentials = "credentials"
    val credentialsOther = "credentialsOther"
    val data = "data"
    val dateModified = "dateModified"
    val demonstratedKnowledge = "demonstratedKnowledge"
    val description = "description"
    val extended = "extended"
    val files = "files"
    val gradeLevel = "gradeLevel"
    val id = "id"
    val itemType = "itemType"
    val keySkills = "keySkills"
    val lexile = "lexile"
    val licenseType = "licenseType"
    val originId = "originId"
    val otherAlignments = "otherAlignments"
    val pValue = "pValue"
    val primarySubject = "primarySubject"
    val priorGradeLevel = "priorGradeLevel"
    val priorUse = "priorUse"
    val priorUseOther = "priorUseOther"
    val published = "published"
    val relatedCurriculum = "relatedCurriculum"
    val relatedSubject = "relatedSubject"
    val reviewsPassed = "reviewsPassed"
    val reviewsPassedOther = "reviewsPassedOther"
    val sharedInCollections = "sharedInCollections"
    val sourceUrl = "sourceUrl"
    val standards = "standards"
    val subjects = "subjects"
    val supportingMaterials = "supportingMaterials"
    val taskInfo = "taskInfo"
    val title = "title"
    val workflow = "workflow"
  }

  lazy val fieldValues = FieldValue.current

  implicit object ItemFormat extends Format[Item] {

    import org.corespring.platform.core.models.item.Item.Keys._

    implicit val ItemViewWrites = ItemView.Writes

    def writes(item: Item) = Json.toJson(ContentView[Item](item, None))

    def reads(json: JsValue) = {
      val item = Item()

      item.collectionId = (json \ collectionId).asOpt[String]

      item.playerDefinition = (json \ "playerDefinition").asOpt[PlayerDefinition]
      item.taskInfo = json.asOpt[TaskInfo]
      item.otherAlignments = json.asOpt[Alignments]
      item.workflow = (json \ workflow).asOpt[Workflow]
      item.contributorDetails = json.asOpt[ContributorDetails]

      item.lexile = (json \ lexile).asOpt[String]
      item.pValue = (json \ pValue).asOpt[String]
      item.originId = (json \ originId).asOpt[String]

      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())

      item.priorUse = (json \ priorUse).asOpt[String]
      item.priorUseOther = (json \ priorUseOther).asOpt[String]
      item.priorGradeLevels = (json \ priorGradeLevel).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(priorGradeLevel)).getOrElse(Seq.empty)
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.reviewsPassedOther = (json \ reviewsPassedOther).asOpt[String]
      item.sharedInCollections = (json \ sharedInCollections).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.standards = (json \ standards).asOpt[Seq[String]].getOrElse(Seq())
      item.data = (json \ data).asOpt[Resource]
      item.published = (json \ published).asOpt[Boolean].getOrElse(false)

      try {
        import org.corespring.platform.core.models.versioning.VersionedIdImplicits.{ Reads => IdReads }
        item.id = (json \ id).asOpt[VersionedId[ObjectId]](IdReads).getOrElse(VersionedId(new ObjectId()))
      } catch {
        case e: Throwable => throw new JsonValidationException(id)
      }
      JsSuccess(item)
    }
  }

}
