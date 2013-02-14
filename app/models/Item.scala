package models

import json.ItemView
import play.api.Play.current
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import controllers._
import scala.Either
import mongoContext._
import com.novus.salat._
import com.mongodb.util.{JSONParseException, JSON}
import controllers.InternalError
import scala.Left
import scala.Right
import play.api.libs.json._
import com.novus.salat._
import dao.{ModelCompanion, SalatDAOUpdateError, SalatDAO, SalatMongoCursor}
import controllers.auth.Permission
import play.api.Logger
import java.util.regex.Pattern
import dao.{SalatDAOUpdateError, SalatDAO, SalatMongoCursor}
import controllers.auth.Permission
import org.joda.time.DateTime
import models.item._


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
                 var workflow: Option[Workflow] = None,
                 var dateModified: Option[DateTime] = Some(new DateTime()),
                 var taskInfo: Option[TaskInfo] = None,
                 var otherAlignments: Option[Alignments] = None,
                 var id: ObjectId = new ObjectId(),
                 var version:Option[Version] = None) extends Content


/**
 * An Item model
 */
object Item extends ModelCompanion[Item,ObjectId]{

  import com.mongodb.casbah.commons.conversions.scala._

  RegisterJodaTimeConversionHelpers()
  val FieldValuesVersion = "0.0.1"

  val collection = Content.collection

  val dao = new SalatDAO[Item, ObjectId](collection = collection) {}

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
  val version = Content.version

  lazy val fieldValues = FieldValue.current
  implicit object ItemWrites extends Writes[Item] {
    def writes(item: Item) = {
      ItemView.ItemViewWrites.writes(ItemView(item,None))
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

      try {
        item.id = (json \ id).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())
      } catch {
        case e: IllegalArgumentException => throw new JsonValidationException(id)
      }

      item
    }
  }

  def updateItem(oid: ObjectId, newItem: Item, fields: Option[DBObject], requesterOrgId: ObjectId): Either[InternalError, Item] = {
    try {

      val copy = newItem.copy(dateModified = Some(new DateTime()))

      val toUpdate = if (!copy.collectionId.isEmpty)
        ((grater[Item].asDBObject(copy) - "_id") - supportingMaterials - data)
      else
        ((grater[Item].asDBObject(copy) - "_id") - supportingMaterials) - collectionId

      Item.update(MongoDBObject("_id" -> oid), MongoDBObject("$set" -> toUpdate), upsert = false, multi = false, wc = Item.collection.writeConcern)

      def getItemWithFields = fields.map(Item.collection.findOneByID(oid, _))
        .getOrElse(Item.collection.findOneByID(oid))
        .map(grater[Item].asObject(_))

      getItemWithFields match {
        case Some(item) => Right(item)
        case None => Left(InternalError("somehow the document that was just updated could not be found", LogType.printFatal))
      }

    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage, LogType.printFatal, false, Some("error occured while updating")))
      case e: IllegalArgumentException => Left(InternalError(e.getMessage, clientOutput = Some("destination collection id was not a valid id")))
      case e: RuntimeException => Left(InternalError(e.getMessage, addMessageToClientOutput = true))
    }
  }

  def cloneItem(item: Item): Option[Item] = {
    val copy = item.copy(id = new ObjectId())
    Item.save(copy)
    Some(copy)
  }

  def countItems(query: DBObject, fields: Option[String] = None): Int = {
    val fieldsDbo = JSON.parse(fields.getOrElse("{}")).asInstanceOf[BasicDBObject]
    val result: SalatMongoCursor[Item] = Item.find(query, fieldsDbo)
    result.count
  }

//  def list(query: MongoDBObject, fields: MongoDBObject, skip: Int = 0, limit: Int = 200) : List[Item] = {
//    val result : SalatMongoCursor[Item] = Item.find( query, fields)
//    result.limit(limit)
//    result.skip(skip)
//    result.toList
//  }

  def queryValueFn(name: String, seq: Seq[KeyValue])(c: Any) = {
    c match {
      case x: String => if (seq.exists(_.key == x)) Right(x) else Left(InternalError("no valid " + name + " found for given value"))
      case _ => Left(InternalError("invalid value format"))
    }
  }

  def getQti(itemId: ObjectId): Either[InternalError, String] = {
    Item.collection.findOneByID(itemId, MongoDBObject(Item.data -> 1)) match {
      case None => Left(InternalError("not found"))
      case Some(o) => o.get(Item.data) match {
        case res: BasicDBObject => {
          grater[Resource].asObject(res).files.find(bf => bf.isMain && bf.contentType == BaseFile.ContentTypes.XML) match {
            case Some(bf) => bf match {
              case vf: VirtualFile => Right(vf.content)
              case _ => Left(InternalError("main file was not a virtual file", LogType.printFatal))
            }
            case None => Left(InternalError("no main file found that contained xml", LogType.printFatal))
          }
        }
        case _ => Left(InternalError("data not an object"))
      }
    }
  }
}
