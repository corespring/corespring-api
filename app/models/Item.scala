package models

import json.ItemView
import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import com.mongodb.casbah.Imports._
import controllers._
import collection.{SeqProxy, mutable}
import scala.Either
import mongoContext._
import com.mongodb.util.{JSONParseException, JSON}
import controllers.InternalError
import scala.Left
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Right
import com.mongodb.QueryBuilder
import web.views.html.partials._edit._metadata._formWithLegend
import com.novus.salat.annotations.raw.Salat
import play.api.libs.json._
import com.novus.salat._
import dao.{ModelCompanion, SalatDAOUpdateError, SalatDAO, SalatMongoCursor}
import models.Workflow.WorkflowWrites
import controllers.auth.Permission
import play.api.Logger
import java.util.regex.Pattern

case class Copyright(owner: Option[String] = None, year: Option[String] = None, expirationDate: Option[String] = None, imageName: Option[String] = None)
object Copyright{
  val owner = "owner"
  val year = "year"
  val expirationDate = "expirationDate"
  val imageName = "imageName"
}

case class Subjects(var primary: Option[ObjectId] = None, var related: Option[ObjectId] = None)
object Subjects{
  val primary = "primary"
  val related = "related"
}

case class ContributorDetails(
                               var contributor: Option[String] = None,
                               var credentials: Option[String] = None,
                               var copyright: Option[Copyright] = None,
                               var author: Option[String] = None,
                               var sourceUrl: Option[String] = None,
                               var licenseType: Option[String] = None,
                               var costForResource: Option[Int] = None
                               )
object ContributorDetails{
  val contributor = "contributor"
  val credentials = "credentials"
  val copyright = "copyright"
  val author = "author"
  val sourceUrl = "sourceUrl"
  val licenseType = "licenseType"
  val costForResource = "costForResource"
}

case class Workflow(var setup: Boolean = false,
                    var tagged: Boolean = false,
                    var standardsAligned: Boolean = false,
                    var qaReview: Boolean = false)

object Workflow{
  val setup: String = "setup"
  val tagged: String = "tagged"
  val standardsAligned: String = "standardsAligned"
  val qaReview: String = "qaReview"

  implicit object WorkflowWrites extends Writes[Workflow] {

    def writes(workflow: Workflow) = {

      JsObject(Seq(
        setup -> JsBoolean(workflow.setup),
        tagged -> JsBoolean(workflow.tagged),
        standardsAligned -> JsBoolean(workflow.standardsAligned),
        qaReview -> JsBoolean(workflow.qaReview)
      ))
    }

  }

  implicit object WorkflowReads extends Reads[Workflow] {
    def reads(json: JsValue): Workflow = {

      Workflow(
        setup = (json \ setup).asOpt[Boolean].getOrElse(false),
        tagged = (json \ tagged).asOpt[Boolean].getOrElse(false),
        standardsAligned = (json \ standardsAligned).asOpt[Boolean].getOrElse(false),
        qaReview = (json \ qaReview).asOpt[Boolean].getOrElse(false)
      )
    }
  }

}


case class Item(var collectionId: String = "",
                var contributorDetails: Option[ContributorDetails] = None,
                var contentType: String = "item",
                var gradeLevel: Seq[String] = Seq(),
                var itemType: Option[String] = None,
                var keySkills: Seq[String] = Seq(),
                var subjects: Option[Subjects] = None,
                var priorUse: Option[String] = None,
                var priorGradeLevel: Seq[String] = Seq(),
                var reviewsPassed: Seq[String] = Seq(),
                var standards: Seq[String] = Seq(),
                var pValue: Option[String] = None,
                var lexile: Option[String] = None,
                var title: Option[String] = None,
                var data: Option[Resource] = None,
                var originId: Option[String] = None,
                var relatedCurriculum: Option[String] = None,
                var demonstratedKnowledge: Option[String] = None,
                var bloomsTaxonomy: Option[String] = None,
                var supportingMaterials: Seq[Resource] = Seq(),
                var workflow: Option[Workflow] = None,
                var id: ObjectId = new ObjectId()) extends Content {
}

/**
 * An Item model
 */
object Item extends ModelCompanion[Item,ObjectId]{
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

  lazy val fieldValues = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).getOrElse(throw new RuntimeException("could not find field values doc with specified version"))

  implicit object ItemWrites extends Writes[Item] {
    def writes(item: Item) = {
      ItemView.ItemViewWrites.writes(ItemView(item,None))
    }
  }

  def getValidatedValue(s: Seq[KeyValue])(json: JsValue, key: String) = {
    (json \ key).asOpt[String]
      .map(v => if (s.exists(_.key == v)) v else throw new JsonValidationException(key))
  }

  implicit object ItemReads extends Reads[Item] {
    def reads(json: JsValue): Item = {
      val item = Item("")
      item.collectionId = (json \ collectionId).asOpt[String].getOrElse("") //must do checking outside of json deserialization
      item.lexile = (json \ lexile).asOpt[String]

      item.workflow = (json \ workflow).asOpt[Workflow]

      item.demonstratedKnowledge = getValidatedValue(fieldValues.demonstratedKnowledge)(json, demonstratedKnowledge)
      item.bloomsTaxonomy = getValidatedValue(fieldValues.bloomsTaxonomy)(json, bloomsTaxonomy)
      item.pValue = (json \ pValue).asOpt[String]
      item.relatedCurriculum = (json \ relatedCurriculum).asOpt[String]

      item.originId = (json \ originId).asOpt[String]

      item.contributorDetails = Some(
        ContributorDetails(
          author = (json \ author).asOpt[String],
          contributor = (json \ contributor).asOpt[String],
          costForResource = (json \ costForResource).asOpt[Int],
          copyright = getCopyright(json),
          sourceUrl = (json \ sourceUrl).asOpt[String],
          licenseType = (json \ licenseType).asOpt[String],
          credentials = (json \ credentials).asOpt[String].
            map(v => if (fieldValues.credentials.exists(_.key == v)) v else throw new JsonValidationException(credentials))
        ))


      def getCopyright(json: JsValue): Option[Copyright] = {
        get[Copyright](
          json,
          Seq(copyrightOwner, copyrightYear, copyrightExpirationDate, copyrightImageName),
          (s: Seq[Option[String]]) => Copyright(s(0), s(1), s(2), s(3)))
      }

      def getSubjects(json: JsValue): Option[Subjects] = {
        try {

          get[Subjects](
            json,
            Seq(primarySubject, relatedSubject),
            (s: Seq[Option[String]]) => s.find(_.isDefined) match {
              case Some(_) => Subjects(s(0).map(new ObjectId(_)), s(1).map(new ObjectId(_)))
              case _ => null
            }
          )
        }
        catch {
          case e: IllegalArgumentException => throw new JsonValidationException(e.getMessage)
        }
      }


      def get[A](json: JsValue, names: Seq[String], fn: (Seq[Option[String]] => A)): Option[A] = {
        val vals: Seq[Option[String]] = names.map((n: String) => {
          (json \ n).asOpt[String]
        })

        vals.find(_.isDefined) match {
          case Some(_) => {
            val result: A = fn(vals)
            if (result == null) {
              None
            } else {
              Some(fn(vals))
            }
          }
          case _ => None
        }
      }

      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())

      (json \ itemType).asOpt[String] match {
        case Some(foundType) => item.itemType = Some(foundType)
        case _ => //do nothing
      }


      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())
      item.gradeLevel = (json \ gradeLevel).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(gradeLevel)).getOrElse(Seq.empty)
      item.keySkills = (json \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty)

      item.subjects = getSubjects(json)

      item.priorUse = (json \ priorUse).asOpt[String]
      item.priorGradeLevel = (json \ priorGradeLevel).asOpt[Seq[String]].
              map(v => if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(priorGradeLevel)).getOrElse(Seq.empty)
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty)
      try {
        item.standards = (json \ standards).asOpt[Seq[String]].getOrElse(Seq.empty)
      } catch {
        case e: IllegalArgumentException => throw new JsonValidationException(standards)
      }
      item.title = (json \ title).asOpt[String]
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
      import com.novus.salat.grater

      //newItem.id = oid
      val toUpdate = if (newItem.collectionId != "") {
        if (ContentCollection.isAuthorized(requesterOrgId, new ObjectId(newItem.collectionId), Permission.Write)) {
          ((grater[Item].asDBObject(newItem) - "_id") - supportingMaterials - data)
        } else throw new RuntimeException("not authorized")
      } else ((grater[Item].asDBObject(newItem) - "_id") - supportingMaterials) - collectionId
      Item.update(MongoDBObject("_id" -> oid), MongoDBObject("$set" -> toUpdate), upsert = false, multi = false, wc = Item.collection.writeConcern)
      fields.map(Item.collection.findOneByID(oid, _)).getOrElse(Item.collection.findOneByID(oid)) match {
        case Some(dbo) => Right(grater[Item].asObject(dbo))
        case None => Left(InternalError("somehow the document that was just updated could not be found", LogType.printFatal))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage, LogType.printFatal, false, Some("error occured while updating")))
      case e: IllegalArgumentException => Left(InternalError(e.getMessage, clientOutput = Some("destination collection id was not a valid id")))
      case e: RuntimeException => Left(InternalError(e.getMessage, addMessageToClientOutput = true))
    }
  }

  def cloneItem(item: Item): Option[Item] = {
    item.id = new ObjectId()
    Item.save(item)
    Some(item)
  }

  def countItems(query: DBObject, fields : Option[String] = None ) : Int = {
    val fieldsDbo = JSON.parse(fields.getOrElse("{}")).asInstanceOf[BasicDBObject]
    val result : SalatMongoCursor[Item] = Item.find( query, fieldsDbo)
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
