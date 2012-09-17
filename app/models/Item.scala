package models

import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import com.novus.salat.dao.{SalatDAOUpdateError, SalatDAO, ModelCompanion}
import com.mongodb.casbah.Imports._
import controllers._
import collection.mutable
import scala.Either
import mongoContext._
import com.mongodb.util.JSON
import controllers.InternalError
import scala.Left
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Right
import com.mongodb.QueryBuilder

//case class ItemFile(var filename:String)
//object ItemFile extends Queryable[ItemFile]{
//  val filename = "filename"
//  implicit object ItemFileWrites extends Writes[ItemFile]{
//    def writes(itemFile:ItemFile) = {
//      JsObject(Seq[(String,JsValue)](filename -> JsString(itemFile.filename)))
//    }
//  }
//  implicit object ItemFileReads extends Reads[ItemFile]{
//    def reads(json:JsValue) = {
//      ItemFile((json \ filename).as[String])
//    }
//  }
//  val queryFields:Seq[QueryField[ItemFile]] = Seq(
//    QueryFieldString(filename,_.filename)
//  )
//}

//case class ItemSubject(subject: String, category: String, refId: String)
//
//object ItemSubject extends Queryable[ItemSubject] {
//  val subject = "subject"
//  val category = "category"
//  val refId = "refId"
//
//  implicit object ItemSubjectWrites extends Writes[ItemSubject] {
//    def writes(itemSubject: ItemSubject) = {
//      //val seq = Seq
//      Json.toJson(Map(subject -> itemSubject.subject, category -> itemSubject.category, refId -> itemSubject.refId))
//    }
//  }
//
//  implicit object ItemSubjectReads extends Reads[ItemSubject] {
//    def reads(json: JsValue) = {
//      ItemSubject((json \ subject).asOpt[String].getOrElse(""),
//        (json \ category).as[String],
//        (json \ refId).as[String])
//    }
//  }
//
//  val queryFields: Seq[QueryField[ItemSubject]] = Seq(
//    QueryFieldString(subject, _.subject),
//    QueryFieldString(category, _.category),
//    QueryFieldString(refId, _.refId)
//  )
//}


import com.novus.salat.annotations.raw.Salat
import play.api.libs.json._

@Salat
abstract class BaseFile(val name: String, val contentType: String, val isMain: Boolean)

object BaseFile {

  object ContentTypes {
    val JPG : String = "image/jpg"
    val PNG : String = "image/png"
    val GIF : String = "image/gif"
    val DOC : String = "application/msword"
    val PDF : String = "application/pdf"
    val XML : String = "text/xml"
    val CSS : String = "text/css"
    val HTML : String = "text/html"
    val TXT : String = "text/txt"
    val JS : String = "text/javascript"
  }

  val SuffixToContentTypes = Map(
    "jpg" -> ContentTypes.JPG,
    "jpeg" -> ContentTypes.JPG,
    "png" -> ContentTypes.PNG,
    "gif" -> ContentTypes.GIF,
    "doc" -> ContentTypes.DOC,
    "pdf" -> ContentTypes.PDF,
    "xml" -> ContentTypes.XML,
    "css" -> ContentTypes.CSS,
    "html" -> ContentTypes.HTML,
    "txt" -> ContentTypes.TXT,
    "js" -> ContentTypes.JS)

  def getContentType(filename: String): String = {
    val split = filename.split("\\.").toList
    val suffix = split.last
    SuffixToContentTypes.getOrElse(suffix, "unknown")
  }

  implicit object BaseFileWrites extends Writes[BaseFile] {
    def writes(f: BaseFile): JsValue = {
      if (f.isInstanceOf[VirtualFile]) {
        VirtualFile.VirtualFileWrites.writes(f.asInstanceOf[VirtualFile])
      } else {
        StoredFile.StoredFileWrites.writes(f.asInstanceOf[StoredFile])
      }
    }
  }

  implicit object BaseFileReads extends Reads[BaseFile] {

    def reads(json: JsValue): BaseFile = {

      val name = (json \ "name").asOpt[String].getOrElse("unknown")
      val contentType = (json \ "contentType").asOpt[String].getOrElse(getContentType(name))
      val isMain = (json\ "default").asOpt[Boolean].getOrElse(false)

      println("gettin here")

      (json\ "content").asOpt[String] match {
        case Some(content) => { println("Content is king: " + content); VirtualFile(name, contentType, isMain, content) }
        case _ => StoredFile(name, contentType, isMain) //we are missing the storageKey here 
      }
    }
  }

  def toJson(f: BaseFile): JsObject = {
    JsObject(Seq(
      "name" -> JsString(f.name),
      "contentType" -> JsString(f.contentType),
      "default" -> JsBoolean(f.isMain))
    )
  }
}

/*
 * A VirtualFile is a representation of a file, but the file contents are stored in mongo.
 * Used for text based files.
 */
case class VirtualFile(override val name: String, override val contentType: String, override val isMain: Boolean = false, var content: String) extends BaseFile(name, contentType, isMain)

object VirtualFile {

  implicit object VirtualFileWrites extends Writes[VirtualFile] {
    def writes(f: VirtualFile): JsValue = {
      BaseFile.toJson(f) ++ JsObject(Seq("content" -> JsString(f.content)))
    }
  }

}


/**
 * A File that has been stored in a file storage service.
 */
case class StoredFile(override val name: String, override val contentType: String, override val isMain: Boolean = false, var storageKey: String = "") extends BaseFile(name, contentType, isMain)

object StoredFile {

  implicit object StoredFileWrites extends Writes[StoredFile] {
    def writes(f: StoredFile): JsValue = {
      BaseFile.toJson(f)
      //"storageKey is for internal use only"
      //++ JsObject(Seq("storageKey" -> JsString(f.storageKey)))
    }
  }

}

/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(name: String, var files: Seq[BaseFile])

object Resource {
  val name = "name"
  val files = "files"

  implicit object ResourceWrites extends Writes[Resource] {
    def writes(res: Resource): JsValue = {
      import BaseFile._
      JsObject(List(
        "name" -> JsString(res.name),
        "files" -> Json.toJson(res.files)
      ))
    }
  }

  implicit object ResourceReads extends Reads[Resource] {
    def reads(json: JsValue): Resource = {
      val resourceName = (json \ "name").as[String]
      val files = (json \ "files").asOpt[Seq[JsValue]].map(_.map(f => {

        val fileName = (f \ "name").as[String]
        val contentType = (f \ "contentType").as[String]
        val isMain = (f \ "default").as[Boolean]
        (f \ "content").asOpt[String] match {
          case Some(c) => VirtualFile(fileName, contentType, isMain, c)
          case _ => StoredFile(fileName, contentType, isMain, (f \ "storageKey").as[String])
        }
      }))
      Resource(resourceName, files.getOrElse(Seq()))
    }
  }

}

case class Item(var collectionId: String = "",
                var contentType: String = "item",
                var author: Option[String] = None,
                var contributor: Option[String] = None,
                var copyrightOwner: Option[String] = None,
                var copyrightYear: Option[String] = None,
                var credentials: Option[String] = None,
                var gradeLevel: Seq[String] = Seq(),
                var itemType: Option[String] = None,
                var itemTypeOther: Option[String] = None,
                var keySkills: Seq[String] = Seq(),
                var licenseType: Option[String] = None,
                var primarySubject: Option[ObjectId] = None,
                var priorUse: Option[String] = None,
                var reviewsPassed: Seq[String] = Seq(),
                var sourceUrl: Option[String] = None,
                var standards: Seq[ObjectId] = Seq(),
                var title: Option[String] = None,
                var data: Option[Resource] = None,
                var supportingMaterials: Seq[Resource] = Seq(),
                var id: ObjectId = new ObjectId()) extends Content {
}

/**
 * An Item model
 */
object Item extends DBQueryable[Item] {
  val FieldValuesVersion = "0.0.1"

  val collection = Content.collection
  val dao = new SalatDAO[Item, ObjectId](collection = collection) {}

  val id = "id"
  val author = "author"
  val collectionId = Content.collectionId
  val contentType = Content.contentType
  val contributor = "contributor"
  val copyrightOwner = "copyrightOwner"
  val copyrightYear = "copyrightYear"
  val credentials = "credentials"
  val files = "files"
  val gradeLevel = "gradeLevel"
  val itemType = "itemType"
  val itemTypeOther = "itemTypeOther"
  val keySkills = "keySkills"
  val licenseType = "licenseType"
  val primarySubject = "primarySubject"
  val priorUse = "priorUse"
  val reviewsPassed = "reviewsPassed"
  val sourceUrl = "sourceUrl"
  val standards = "standards"
  val title = "title"
  //val xmlData = "xmlData"
  val data = "data"
  val supportingMaterials = "supportingMaterials"

  lazy val fieldValues = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).getOrElse(throw new RuntimeException("could not find field values doc with specified version"))

  implicit object ItemWrites extends Writes[Item] {
    def writes(item: Item) = {
      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(item.id.toString))
      item.author.foreach(v => iseq = iseq :+ (author -> JsString(v)))
      iseq = iseq :+ (collectionId -> JsString(item.collectionId))
      iseq = iseq :+ (contentType -> JsString(ContentType.item))
      item.contributor.foreach(v => iseq = iseq :+ (contributor -> JsString(v)))
      item.copyrightOwner.foreach(v => iseq = iseq :+ (copyrightOwner -> JsString(v)))
      item.copyrightYear.foreach(v => iseq = iseq :+ (copyrightYear -> JsString(v)))
      item.credentials.foreach(v => iseq = iseq :+ (credentials -> JsString(v)))
      if (!item.supportingMaterials.isEmpty) iseq = iseq :+ (supportingMaterials -> JsArray(item.supportingMaterials.map(Json.toJson(_))))
      if (!item.gradeLevel.isEmpty) iseq = iseq :+ (gradeLevel -> JsArray(item.gradeLevel.map(JsString(_))))
      item.itemType.foreach(v => iseq = iseq :+ (itemType -> JsString(v)))
      item.itemTypeOther.foreach(v => iseq = iseq :+ (itemTypeOther -> JsString(v)))
      if (!item.keySkills.isEmpty) iseq = iseq :+ (keySkills -> JsArray(item.keySkills.map(JsString(_))))
      item.licenseType.foreach(v => iseq = iseq :+ (licenseType -> JsString(v)))
      if (!item.primarySubject.isEmpty) {
        val subject = Subject.findOneById(item.primarySubject.get)
        iseq = iseq :+ (primarySubject -> Json.toJson(subject))
      }
      item.priorUse.foreach(v => iseq = iseq :+ (priorUse -> JsString(v)))
      if (!item.reviewsPassed.isEmpty) iseq = iseq :+ (reviewsPassed -> JsArray(item.reviewsPassed.map(JsString(_))))
      item.sourceUrl.foreach(v => iseq = iseq :+ (sourceUrl -> JsString(v)))
      if (!item.standards.isEmpty) iseq = iseq :+ (standards -> Json.toJson(item.standards.
        foldRight[Seq[Standard]](Seq[Standard]())((sid, acc) => Standard.findOneById(sid) match {
        case Some(standard) => acc :+ standard
        case None => Log.f("ItemWrites: no standard found given id"); acc
      })))
      item.title.foreach(v => iseq = iseq :+ (title -> JsString(v)))
      item.data.foreach(v => iseq = iseq :+ (data -> Json.toJson(v)))
      JsObject(iseq)
    }
  }

  implicit object ItemReads extends Reads[Item] {
    def reads(json: JsValue): Item = {
      val item = Item("")
      item.collectionId = (json \ collectionId).asOpt[String].getOrElse("") //must do checking outside of json deserialization
      item.author = (json \ author).asOpt[String]
      item.contributor = (json \ contributor).asOpt[String]
      item.copyrightOwner = (json \ copyrightOwner).asOpt[String]
      item.copyrightYear = (json \ copyrightYear).asOpt[String]
      item.credentials = (json \ credentials).asOpt[String]
      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())
      item.gradeLevel = (json \ gradeLevel).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.itemType = (json \ itemType).asOpt[String]
      item.credentials = (json \ credentials).asOpt[String].
        map(v => if (fieldValues.credentials.exists(_.key == v)) v else throw new JsonValidationException(credentials))
      item.supportingMaterials = (json \ supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq())
      item.gradeLevel = (json \ gradeLevel).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(gradeLevel)).getOrElse(Seq.empty)
      item.itemType = (json \ itemType).asOpt[String].
        map(v => if (fieldValues.itemTypes.exists(_.key == v)) v else throw new JsonValidationException(itemType))
      item.itemTypeOther = (json \ itemTypeOther).asOpt[String]
      item.keySkills = (json \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.licenseType = (json \ licenseType).asOpt[String]
      try{
      item.primarySubject = (json \ primarySubject).asOpt[String].map(new ObjectId(_))
      }catch{
        case e:IllegalArgumentException => throw new JsonValidationException(primarySubject)
      }
      item.priorUse = (json \ priorUse).asOpt[String]
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.sourceUrl = (json \ sourceUrl).asOpt[String]
      try {
        item.standards = (json \ standards).asOpt[Seq[String]].map(_.map(new ObjectId(_))).getOrElse(Seq.empty)
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

  def updateItem(oid: ObjectId, newItem: Item): Either[InternalError, Item] = {
    try {
      import com.novus.salat.grater
      //newItem.id = oid
      val toUpdate = (( grater[Item].asDBObject(newItem) - "_id" ) - supportingMaterials ) - data
      Item.update(MongoDBObject("_id" -> oid), MongoDBObject("$set" -> toUpdate), upsert = false, multi = false, wc = Item.collection.writeConcern)
      Item.findOneById(oid) match {
        case Some(i) => Right(i)
        case None => Left(InternalError("somehow the document that was just updated could not be found", LogType.printFatal))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage, LogType.printFatal, false, Some("error occured while updating")))
    }
  }

  val queryFields: Seq[QueryField[Item]] = Seq[QueryField[Item]](
    QueryFieldObject[Item](id, _.id, QueryField.valuefuncid),
    QueryFieldString[Item](author, _.author),
    QueryFieldString[Item](collectionId, _.collectionId),
    QueryFieldString[Item](contentType, _.contentType, _ match {
      case x: String if x == ContentType.item => Right(x)
      case _ => Left(InternalError("incorrect content type"))
    }),
    QueryFieldString[Item](contributor, _.contributor),
    QueryFieldString[Item](copyrightOwner, _.copyrightOwner),
    QueryFieldString[Item](copyrightYear, _.copyrightYear),
    QueryFieldString[Item](credentials, _.credentials, value => {
      value match {
        case x: String => if (fieldValues.credentials.exists(_.key == x)) Right(x) else Left(InternalError("no valid credentials found for given value"))
        case _ => Left(InternalError("invalid value format"))
      }
    }),
    QueryFieldStringArray[Item](gradeLevel, _.gradeLevel, value => {
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
    QueryFieldString[Item](itemType, _.itemType, value => {
      value match {
        case x: String => if (fieldValues.itemTypes.exists(_.key == x)) Right(value) else Left(InternalError("could not find valid item types for value"))
        case _ => Left(InternalError("invalid type for value in itemType"))
      }
    }),
    QueryFieldString[Item](itemTypeOther, _.itemTypeOther),
    QueryFieldStringArray[Item](keySkills, _.keySkills, value => value match {
      case skills: BasicDBList => if (skills.foldRight[Boolean](true)((skill, acc) => fieldValues.keySkills.exists(_.key == skill.toString) && acc)) Right(value)
      else Left(InternalError("key skill not found for given value"))
      case _ => Left(InternalError("invalid value type for keySkills"))
    }
    ),
    QueryFieldString[Item](licenseType, _.licenseType, value => value match {
      case x: String => if (fieldValues.licenseTypes.exists(_.key == x)) Right(value) else Left(InternalError("license type not found"))
      case _ => Left(InternalError("invalid value type for licenceType"))
    }
    ),
    QueryFieldObject[Item](primarySubject, _.primarySubject, _ match {
      case x: BasicDBList => x.foldRight[Either[InternalError, Seq[ObjectId]]](Right(Seq()))((subject, acc) => {
        acc match {
          case Right(ids) => subject match {
            case x: String => try {
              Right(ids :+ new ObjectId(x))
            } catch {
              case e: IllegalArgumentException => Left(InternalError("invalid object id format for primarySubject"))
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
    QueryFieldString[Item](sourceUrl, _.sourceUrl),
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
    QueryFieldString[Item](title, _.title)
  )

  override def preParse(dbo: DBObject): QueryParser = {
//    val qp = QueryParser.buildQuery(dbo, QueryParser(), Seq(queryFields.find(_.key == standards).get))
//    qp.result match {
//      case Right(query) =>
//        val dbquery = query.result()
//        QueryParser.replaceKeys(dbquery, Standard.queryFields.map(qf => standards + "." + qf.key -> qf.key))
//        val builder = MongoDBObject.newBuilder
//        if (!dbquery.isEmpty) {
//          val c = Standard.find(dbquery, MongoDBObject("_id" -> 1))
//          val builderList = MongoDBList.newBuilder
//          if (!c.isEmpty) {
//            c.foreach(builderList += _.id)
//            builder += (standards -> MongoDBObject("$in" -> builderList.result()))
//          }
//        }
//        QueryParser.removeKeys(dbo, Standard.queryFields.foldRight[Seq[String]](Seq(standards))((qf, acc) => acc :+ standards + "." + qf.key))
//        QueryParser(Right(builder))
//      case Left(e) => QueryParser(Left(e))
//    }
    parseProperty[Standard](dbo,standards,Standard) match {
      case Right(builder1) => parseProperty[Subject](dbo,primarySubject,Subject) match {
        case Right(builder2) =>
          val builder = MongoDBObject.newBuilder
          builder1.foreach(field => builder += field)
          builder2.foreach(field => builder += field)
          QueryParser(Right(builder))
        case Left(e) => QueryParser(Left(e))
      }
      case Left(e) => QueryParser(Left(e))
    }
  }
  private def parseProperty[T <: Identifiable](dbo: DBObject, key:String, joinedQueryable:DBQueryable[T]):Either[InternalError,Seq[(String,Any)]] = {
    val qp = QueryParser.buildQuery(dbo, QueryParser(), Seq(queryFields.find(_.key == key).get))
    qp.result match {
      case Right(query) =>
        val dbquery = query.result()
        QueryParser.replaceKeys(dbquery, joinedQueryable.queryFields.map(qf => key + "." + qf.key -> qf.key))
        var builder:Seq[(String,Any)] = Seq()
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
