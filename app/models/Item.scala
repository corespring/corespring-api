package models

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

case class Subjects(var primary: Option[ObjectId] = None, var related: Option[ObjectId] = None)

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

object Workflow extends Searchable{
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

  def toSearchObj(query:AnyRef):Either[InternalError,MongoDBObject] = {
    def isBooleanCheck(key:String,value:AnyRef,searchobj:MongoDBObject):Either[InternalError,MongoDBObject] = if(value.isInstanceOf[Boolean]){
      Right(searchobj += key -> value)
    } else {
      Left(InternalError("Workflow.setup did not have value of type boolean",addMessageToClientOutput = true))
    }
    query match {
      case dbquery:BasicDBObject => dbquery.foldRight[Either[InternalError,MongoDBObject]](Right(MongoDBObject()))((field,result) => result match {
        case Right(searchobj) => field._1 match {
          case Workflow.setup => isBooleanCheck(Workflow.setup,field._2,searchobj)
          case Workflow.tagged => isBooleanCheck(Workflow.tagged,field._2,searchobj)
          case Workflow.standardsAligned => isBooleanCheck(Workflow.standardsAligned,field._2,searchobj)
          case Workflow.qaReview => isBooleanCheck(Workflow.qaReview,field._2,searchobj)
        }
        case Left(e) => Left(e)
      })
      case _ => Left(InternalError("invalid search object",LogType.printFatal,addMessageToClientOutput = true))
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
object Item extends ModelCompanion[Item,ObjectId] with Searchable{
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

      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(item.id.toString))

      if (item.workflow.isDefined) iseq = iseq :+ (workflow -> WorkflowWrites.writes(item.workflow.get))

      //ContributorDetails
      item.contributorDetails match {
        case Some(cd) => {
          cd.author.foreach(v => iseq = iseq :+ (author -> JsString(v)))
          cd.contributor.foreach(v => iseq = iseq :+ (contributor -> JsString(v)))
          cd.costForResource.foreach(v => iseq = iseq :+ (costForResource -> JsNumber(v)))
          cd.credentials.foreach(v => iseq = iseq :+ (credentials -> JsString(v)))
          cd.licenseType.foreach(v => iseq = iseq :+ (licenseType -> JsString(v)))
          cd.sourceUrl.foreach(v => iseq = iseq :+ (sourceUrl -> JsString(v)))
          cd.copyright match {
            case Some(c) => {
              c.owner.foreach(v => iseq = iseq :+ (copyrightOwner -> JsString(v)))
              c.year.foreach(v => iseq = iseq :+ (copyrightYear -> JsString(v)))
              c.expirationDate.foreach(v => iseq = iseq :+ (copyrightExpirationDate -> JsString(v)))
              c.imageName.foreach(v => iseq = iseq :+ (copyrightImageName -> JsString(v)))
            }
            case _ => //do nothing
          }
        }
        case _ => //do nothing
      }

      item.lexile.foreach(v => iseq = iseq :+ (lexile -> JsString(v)))

      item.demonstratedKnowledge.foreach(v => iseq = iseq :+ (demonstratedKnowledge -> JsString(v)))

      if (item.originId.isDefined) iseq = iseq :+ (originId -> JsString(item.originId.get))

      iseq = iseq :+ (collectionId -> JsString(item.collectionId))
      iseq = iseq :+ (contentType -> JsString(ContentType.item))
      item.pValue.foreach(v => iseq = iseq :+ (pValue -> JsString(v)))
      item.relatedCurriculum.foreach(v => iseq = iseq :+ (relatedCurriculum -> JsString(v)))

      item.bloomsTaxonomy.foreach(v => iseq = iseq :+ (bloomsTaxonomy -> JsString(v)))

      if (!item.supportingMaterials.isEmpty) iseq = iseq :+ (supportingMaterials -> JsArray(item.supportingMaterials.map(Json.toJson(_))))
      if (!item.gradeLevel.isEmpty) iseq = iseq :+ (gradeLevel -> JsArray(item.gradeLevel.map(JsString(_))))
      item.itemType.foreach(v => iseq = iseq :+ (itemType -> JsString(v)))
      if (!item.keySkills.isEmpty) iseq = iseq :+ (keySkills -> JsArray(item.keySkills.map(JsString(_))))


      item.subjects match {
        case Some(s) => {

          def getSubject(id: Option[ObjectId]): Option[JsValue] = id match {
            case Some(foundId) => {
              Subject.findOneById(foundId) match {
                case Some(subj) => Some(Json.toJson(subj))
                case _ => throw new RuntimeException("Can't find subject with id: " + foundId + " in item: " + item.id)
              }
            }
            case _ => None
          }

          var seqsubjects: Seq[(String, JsValue)] = Seq()

          getSubject(s.primary) match {
            case Some(found) => iseq = iseq :+ (primarySubject -> Json.toJson(found))
            case _ => //do nothing
          }
          getSubject(s.related) match {
            case Some(found) => iseq = iseq :+ (relatedSubject -> Json.toJson(found))
            case _ => //do nothing
          }
        }
        case _ => //
      }

      item.priorUse.foreach(v => iseq = iseq :+ (priorUse -> JsString(v)))
      if (!item.priorGradeLevel.isEmpty) iseq = iseq :+ (priorGradeLevel -> JsArray(item.priorGradeLevel.map(JsString(_))))
      if (!item.reviewsPassed.isEmpty) iseq = iseq :+ (reviewsPassed -> JsArray(item.reviewsPassed.map(JsString(_))))
      if (!item.standards.isEmpty) iseq = iseq :+ (standards -> Json.toJson(item.standards.
        foldRight[Seq[Standard]](Seq[Standard]())((dn, acc) => Standard.findOne( MongoDBObject("dotNotation" -> dn)) match {
        case Some(standard) => acc :+ standard
        case None => {
          //throw new RuntimeException("ItemWrites: no standard found given id: " + sid); acc
          Logger.warn("no standard found for id: " + dn + ", item id: " + item.id )
          acc
          }
      })))
      item.title.foreach(v => iseq = iseq :+ (title -> JsString(v)))
      item.data.foreach(v => iseq = iseq :+ (data -> Json.toJson(v)))
      JsObject(iseq)
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

  def list(query: DBObject, fields: BasicDBObject = new BasicDBObject(), skip: Int = 0, limit: Int = 200) : List[Item] = {
    val result : SalatMongoCursor[Item] = Item.find( query, fields)
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

  def toSearchObj(query: AnyRef):Either[InternalError,MongoDBObject] = {
    query match {
      case strquery:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strquery).asInstanceOf[BasicDBObject]
        toSearchObj(parsedobj)
      }catch {
        case e:JSONParseException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))
      }
      case dbquery:BasicDBObject => {
        dbquery.foldRight[Either[InternalError,MongoDBObject]](Right(MongoDBObject()))((field,result) => result match {
          case Right(searchobj) => field._1 match {
            case Item.workflow => Workflow.toSearchObj(field._2) match {
              case Right(subobj) => Right(searchobj ++ subobj.asDBObject)
              case Left(e) => Left(e)
            }
            case Item.author => field._2 match {
              case strval:String => Right(searchobj += Item.contributorDetails+"."+ContributorDetails.author -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
              case _ => Left(InternalError("invalid value when parsing search for author",addMessageToClientOutput = true))
            }
            case Item.contributor => field._2 match {
              case strval:String => Right(searchobj += Item.contributorDetails+"."+ContributorDetails.contributor -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
              case _ => Left(InternalError("invalid value when parsing search for contributor",addMessageToClientOutput = true))
            }
            case Item.costForResource => field._2 match {
              case strval:String => Right(searchobj += Item.contributorDetails+"."+ContributorDetails.costForResource -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
              case _ => Left(InternalError("invalid value when parsing search for costForResource",addMessageToClientOutput = true))
            }
            case Item.credentials => field._2 match {
              case strval:String => Right(searchobj += Item.contributorDetails+"."+ContributorDetails.credentials -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
              case _ => Left(InternalError("invalid value when parsing search for credentials",addMessageToClientOutput = true))
            }
            case Item.licenseType => field._2 match {
              case strval:String => Right(searchobj += Item.contributorDetails+"."+ContributorDetails.licenseType -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
              case _ => Left(InternalError("invalid value when parsing search for licenseType",addMessageToClientOutput = true))
            }
            case Item.sourceUrl => field._2 match {
              case strval:String => Right(searchobj += Item.contributorDetails+"."+ContributorDetails.sourceUrl -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
              case _ => Left(InternalError("invalid value when parsing search for sourceUrl",addMessageToClientOutput = true))
            }
            case Item.copyrightOwner => Right(searchobj)
            case Item.copyrightYear => Right(searchobj)
            case Item.copyrightExpirationDate => Right(searchobj)
            case Item.copyrightImageName => Right(searchobj)
            case Item.lexile => Right(searchobj)
            case Item.demonstratedKnowledge => Right(searchobj)
            case Item.originId => Right(searchobj)
            case Item.collectionId => Right(searchobj)
            case Item.contentType => Right(searchobj)
            case Item.pValue => Right(searchobj)
            case Item.relatedCurriculum => Right(searchobj)
            case Item.supportingMaterials => Right(searchobj)
            case Item.gradeLevel => Right(searchobj)
            case Item.itemType => Right(searchobj)
            case Item.keySkills => Right(searchobj)
            case Item.primarySubject => Right(searchobj)
            case Item.relatedSubject => Right(searchobj)
            case Item.priorUse => Right(searchobj)
            case Item.priorGradeLevel => Right(searchobj)
            case Item.reviewsPassed => Right(searchobj)
            case Item.standards => Right(searchobj)
          }
          case Left(e) => Left(e)
        })
      }
      case _ => Left(InternalError("invalid search object",LogType.printFatal,addMessageToClientOutput = true))
    }

  }
}
