package models

import com.mongodb.casbah.Imports._
import controllers._
import scala.Either
import mongoContext._
import com.mongodb.util.JSON
import controllers.InternalError
import scala.Left
import scala.Right
import play.api.libs.json._
import com.novus.salat._
import dao.{SalatDAOUpdateError, SalatDAO, SalatMongoCursor}
import controllers.auth.Permission
import play.api.Logger
import org.joda.time.DateTime
import models.item._
import collection.SeqProxy


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
                 var id: ObjectId = new ObjectId()) extends Content {
}

/**
 * An Item model
 */
object Item extends DBQueryable[Item] {

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
  val dateModified = "dateModified"

  lazy val fieldValues = FieldValue.current

  implicit object ItemWrites extends Writes[Item] {

    def writes(item: Item): JsValue = {

      import models.item.Workflow._

      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(item.id.toString))
      iseq = iseq :+ (collectionId -> JsString(item.collectionId))

      if (item.workflow.isDefined) iseq = iseq :+ (workflow -> WorkflowWrites.writes(item.workflow.get))

      val details: Seq[(String, JsValue)] = item.contributorDetails.map(ContributorDetails.json).getOrElse(Seq())
      val taskInfo: Seq[(String, JsValue)] = item.taskInfo.map(TaskInfo.json).getOrElse(Seq())
      val alignments: Seq[(String, JsValue)] = item.otherAlignments.map(Alignments.json).getOrElse(Seq())

      item.lexile.foreach(v => iseq = iseq :+ (lexile -> JsString(v)))

      if (item.originId.isDefined) iseq = iseq :+ (originId -> JsString(item.originId.get))

      iseq = iseq :+ (contentType -> JsString(ContentType.item))
      item.pValue.foreach(v => iseq = iseq :+ (pValue -> JsString(v)))

      if (!item.supportingMaterials.isEmpty) iseq = iseq :+ (supportingMaterials -> JsArray(item.supportingMaterials.map(Json.toJson(_))))

      item.priorUse.foreach(v => iseq = iseq :+ (priorUse -> JsString(v)))
      if (!item.priorGradeLevel.isEmpty) iseq = iseq :+ (priorGradeLevel -> JsArray(item.priorGradeLevel.map(JsString(_))))
      if (!item.reviewsPassed.isEmpty) iseq = iseq :+ (reviewsPassed -> JsArray(item.reviewsPassed.map(JsString(_))))
      if (!item.standards.isEmpty) iseq = iseq :+ (standards -> Json.toJson(item.standards.
        foldRight[Seq[Standard]](Seq[Standard]())((dn, acc) => Standard.findOne(MongoDBObject("dotNotation" -> dn)) match {
        case Some(standard) => acc :+ standard
        case None => {
          //throw new RuntimeException("ItemWrites: no standard found given id: " + sid); acc
          Logger.warn("no standard found for id: " + dn + ", item id: " + item.id)
          acc
        }
      })))
      item.data.foreach(v => iseq = iseq :+ (data -> Json.toJson(v)))
      val finalSeq: Seq[(String, JsValue)] = (iseq ++ details ++ taskInfo ++ alignments)
      JsObject(finalSeq)
    }
  }

  def getValidatedValue(s: Seq[KeyValue])(json: JsValue, key: String) = {
    (json \ key).asOpt[String]
      .map(v => if (s.exists(_.key == v)) v else throw new JsonValidationException(key))
  }

  implicit object ItemReads extends Reads[Item] {
    def reads(json: JsValue): Item = {
      val item = Item()

      /**
       * must do checking outside of json deserialization
       */
      item.collectionId = (json \ collectionId).asOpt[String].getOrElse("")

      item.taskInfo = TaskInfo.obj(json)
      item.otherAlignments = Alignments.obj(json)
      item.workflow = (json \ workflow).asOpt[Workflow]
      item.contributorDetails = ContributorDetails.obj(json)

      item.lexile = (json \ lexile).asOpt[String]
      item.pValue = (json \ pValue).asOpt[String]
      item.originId = (json \ originId).asOpt[String]

      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())

      item.priorUse = (json \ priorUse).asOpt[String]
      item.priorGradeLevel = (json \ priorGradeLevel).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(priorGradeLevel)).getOrElse(Seq.empty)
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty)

      try {
        item.standards = (json \ standards).asOpt[Seq[String]].getOrElse(Seq.empty)
      } catch {
        case e: IllegalArgumentException => throw new JsonValidationException(standards)
      }
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

      def isAuthorized = ContentCollection.isAuthorized(requesterOrgId, new ObjectId(newItem.collectionId), Permission.Write)

      if (!newItem.collectionId.isEmpty && !isAuthorized) {
        throw new RuntimeException("collection not authorized: " + newItem.collectionId + ", orgId: " + requesterOrgId)
      }

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

  def list(query: DBObject, fields: BasicDBObject = new BasicDBObject(), skip: Int = 0, limit: Int = 200): List[Item] = {
    val result: SalatMongoCursor[Item] = Item.find(query, fields)
    result.limit(limit)
    result.skip(skip)
    result.toList
  }

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

  val queryFields: Seq[QueryField[Item]] = Seq[QueryField[Item]](
    QueryFieldObject[Item](id, _.id, QueryField.valuefuncid),
    QueryFieldString[Item](author, _.contributorDetails.map(_.author)),
    QueryFieldString[Item](collectionId, _.collectionId),
    QueryFieldString[Item](originId, _.originId),
    QueryFieldString[Item](contentType, _.contentType, _ match {
      case x: String if x == ContentType.item => Right(x)
      case _ => Left(InternalError("incorrect content type"))
    }),
    QueryFieldString[Item](contributor, _.contributorDetails.map(_.contributor)),
    QueryFieldString[Item](copyrightOwner, _.contributorDetails.map(_.copyright.map(_.owner))),
    QueryFieldString[Item](copyrightYear, _.contributorDetails.map(_.copyright.map(_.year))),
    QueryFieldString[Item](credentials, _.contributorDetails.map(_.credentials), queryValueFn("credentials", fieldValues.credentials)),
    QueryFieldStringArray[Item](gradeLevel, _.taskInfo.map(_.gradeLevel), value => {
      value match {
        case grades: BasicDBList =>
          if (grades.foldRight[Boolean](true)((grade, acc) => fieldValues.gradeLevels.exists(_.key == grade.toString) && acc)) Right(value)
          else Left(InternalError("gradeLevel contained invalid grade formats for values"))
        case grade: String =>
          if (fieldValues.gradeLevels.exists(_.key == grade.toString)) Right(grade) else Left(InternalError("gradeLevel contained invalid grade formats for values"))
        case _ =>
          Left(InternalError("invalid type for value in gradeLevel"))
      }
    }),

    QueryFieldString[Item](workflow + "." + Workflow.qaReview, _.workflow.map(_.qaReview)),
    QueryFieldString[Item](workflow + "." + Workflow.setup, _.workflow.map(_.setup)),
    QueryFieldString[Item](workflow + "." + Workflow.tagged, _.workflow.map(_.tagged)),
    QueryFieldString[Item](workflow + "." + Workflow.standardsAligned, _.workflow.map(_.standardsAligned)),

    /**
     * TODO: Check with Evan/Josh about item types.
     */
    QueryFieldString[Item](itemType, _.taskInfo.map(_.itemType)),
    QueryFieldStringArray[Item](keySkills, _.otherAlignments.map(_.keySkills), value => value match {
      case skills: BasicDBList => if (skills.foldRight[Boolean](true)((skill, acc) => fieldValues.keySkills.exists(_.key == skill.toString) && acc)) Right(value)
      else Left(InternalError("key skill not found for given value"))
      case _ => Left(InternalError("invalid value type for keySkills"))
    }
    ),
    QueryFieldString[Item](bloomsTaxonomy, _.otherAlignments.map(_.bloomsTaxonomy), queryValueFn(bloomsTaxonomy, fieldValues.bloomsTaxonomy)),
    QueryFieldString[Item](licenseType, _.contributorDetails.map(_.licenseType), queryValueFn(licenseType, fieldValues.licenseTypes)),
    QueryFieldObject[Item](primarySubject, _.taskInfo.map(_.subjects.map(_.primary)), _ match {
      case x: String => try {
        Right(new ObjectId(x))
      } catch {
        case e: IllegalArgumentException => Left(InternalError("invalid object id format for primarySubject"))
      }
      case _ => Left(InternalError("uknown value type for standards"))
    }, Subject.queryFields),
    QueryFieldObject[Item](relatedSubject, _.taskInfo.map(_.subjects.map(_.related)), _ match {
      case x: String => try {
        Right(new ObjectId(x))
      } catch {
        case e: IllegalArgumentException => Left(InternalError("invalid object id format for relatedSubject"))
      }
      case _ => Left(InternalError("uknown value type for standards"))
    }, Subject.queryFields),
    QueryFieldStringArray[Item](priorUse, _.priorUse, value => value match {
      case x: String => if (fieldValues.priorUses.exists(_.key == x)) Right(value) else Left(InternalError("priorUse not found"))
      case _ => Left(InternalError("invalid value type for priorUse"))
    }
    ),
    QueryFieldStringArray[Item](reviewsPassed, _.reviewsPassed, value => value match {
      case reviews: BasicDBList => if (reviews.foldRight[Boolean](true)((review, acc) => fieldValues.reviewsPassed.exists(_.key == review.toString) && acc)) Right(value)
      else Left(InternalError("review not found"))
      case _ => Left(InternalError("invalid value type for reviewsPassed"))
    }
    ),
    QueryFieldString[Item](sourceUrl, _.contributorDetails.map(_.sourceUrl)),
    QueryFieldObjectArray[Item](standards, _.standards, _ match {
      case x: BasicDBList => x.foldRight[Either[InternalError, Seq[ObjectId]]](Right(Seq()))((standard, acc) => {
        acc match {
          case Right(ids) => standard match {
            case x: String => try {
              Right(ids :+ new ObjectId(x))
            } catch {
              case e: IllegalArgumentException => Left(InternalError("invalid object id format for standards"))
            }
          }
          case Left(e) => Left(e)
        }
      })
      case x: String => try {
        Right(new ObjectId(x))
      } catch {
        case e: IllegalArgumentException => Left(InternalError("invalid object id format for standards"))
      }
      case _ => Left(InternalError("uknown value type for standards"))
    }, Standard.queryFields),
    QueryFieldString[Item](title, _.taskInfo.map(_.title)),
    QueryFieldString[Item](lexile, _.lexile),
    QueryFieldString[Item](demonstratedKnowledge, _.otherAlignments.map(_.demonstratedKnowledge), queryValueFn(demonstratedKnowledge, fieldValues.demonstratedKnowledge)),
    QueryFieldString[Item](pValue, _.pValue),
    QueryFieldString[Item](relatedCurriculum, _.otherAlignments.map(_.relatedCurriculum))
  )

  override def preParse(dbo: DBObject): QueryParser = {
    parseProperty[Standard](dbo, standards, Standard) match {
      case Right(builder1) =>
        parseProperty[Subject](dbo, primarySubject, Subject) match {
          case Right(builder2) =>
            parseProperty[Subject](dbo, relatedSubject, Subject) match {
              case Right(builder3) => {
                val builder = MongoDBObject.newBuilder
                builder1.foreach(field => builder += field)
                builder2.foreach(field => builder += field)
                builder3.foreach(field => builder += field)
                QueryParser(Right(builder))
              }
              case Left(e) => QueryParser(Left(e))
            }
          case Left(e) => QueryParser(Left(e))
        }
      case Left(e) => QueryParser(Left(e))
    }
  }

  private def parseProperty[T <: Identifiable](dbo: DBObject, key: String, joinedQueryable: DBQueryable[T]): Either[InternalError, Seq[(String, Any)]] = {
    val qp = QueryParser.buildQuery(dbo, QueryParser(), Seq(queryFields.find(_.key == key).get))
    qp.result match {
      case Right(query) =>
        val dbquery = query.result()
        QueryParser.replaceKeys(dbquery, joinedQueryable.queryFields.map(qf => key + "." + qf.key -> qf.key))
        var builder: Seq[(String, Any)] = Seq()
        if (!dbquery.isEmpty) {
          val c = joinedQueryable.find(dbquery, MongoDBObject("_id" -> 1))
          val builderList = MongoDBList.newBuilder
          if (!c.isEmpty) {
            c.foreach(builderList += _.id)
            builder = builder :+ (key -> MongoDBObject("$in" -> builderList.result()))
          }
        }
        QueryParser.removeKeys(dbo, joinedQueryable.queryFields.foldRight[Seq[String]](Seq(key))((qf, acc) => acc :+ key + "." + qf.key))
        Right(builder)
      case Left(e) => Left(e)
    }
  }
}
