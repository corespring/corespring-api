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
import play.api.libs.json.JsObject
import org.specs2.internal.scalaz.Digit._0

case class ItemFile(var filename:String)
object ItemFile extends Queryable[ItemFile]{
  val filename = "filename"
  implicit object ItemFileWrites extends Writes[ItemFile]{
    def writes(itemFile:ItemFile) = {
      JsObject(Seq[(String,JsValue)](filename -> JsString(itemFile.filename)))
    }
  }
  implicit object ItemFileReads extends Reads[ItemFile]{
    def reads(json:JsValue) = {
      ItemFile((json \ filename).as[String])
    }
  }
  val queryFields:Seq[QueryField[ItemFile]] = Seq(
    QueryFieldString(filename,_.filename)
  )
}

case class ItemSubject(var subject:String, var category:String, var refId:String)
object ItemSubject extends Queryable[ItemSubject]{
  val subject = "subject"
  val category = "category"
  val refId = "refId"
  implicit object ItemSubjectWrites extends Writes[ItemSubject]{
    def writes(itemSubject:ItemSubject) = {
      JsObject(Seq[(String,JsValue)](subject -> JsString(itemSubject.subject), category -> JsString(itemSubject.category), refId -> JsString(itemSubject.refId)))
    }
  }
  implicit object ItemSubjectReads extends Reads[ItemSubject]{
    def reads(json:JsValue) = {
      ItemSubject((json \ subject).asOpt[String].getOrElse(""),
        (json \ category).as[String],
        (json \ refId).as[String])
    }
  }
  val queryFields:Seq[QueryField[ItemSubject]] = Seq(
    QueryFieldString(subject,_.subject),
    QueryFieldString(category,_.category),
    QueryFieldString(refId,_.refId)
  )
}

case class Item( var collectionId:String = "",
                 var contentType:String = "item",
                 var author:Option[String] = None,
                var contributor:Option[String] = None,
                var copyrightOwner:Option[String] = None,
                var copyrightYear:Option[String] = None,
                var credentials:Option[String] = None,
                var files:Seq[ItemFile] = Seq(),
                var gradeLevel:Seq[String] = Seq(),
                var itemType:Option[String] = None,
                var itemTypeOther:Option[String] = None,
                var keySkills:Seq[String] = Seq(),
                var licenseType:Option[String] = None,
                var primarySubject:Option[ItemSubject] = None,
                var priorUse:Option[String] = None,
                var reviewsPassed:Seq[String] = Seq(),
                var sourceUrl:Option[String] = None,
                var standards:Seq[ObjectId] = Seq(),
                var title:Option[String] = None,
                var xmlData:Option[String] = None,
                var id:ObjectId = new ObjectId()) extends Content{
}
/**
 * An Item model
 */
object Item extends DBQueryable[Item]{
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
  val xmlData = "xmlData"

  implicit object ItemWrites extends Writes[Item] {
    def writes(item: Item) = {
      var iseq:Seq[(String,JsValue)] = Seq("id" -> JsString(item.id.toString))
      item.author.foreach(v => iseq = iseq :+ (author -> JsString(v)))
      iseq = iseq :+ (collectionId -> JsString(item.collectionId))
      iseq = iseq :+ (contentType -> JsString(ContentType.item))
      item.contributor.foreach(v => iseq = iseq :+ (contributor -> JsString(v)))
      item.copyrightOwner.foreach(v => iseq = iseq :+ (copyrightOwner -> JsString(v)))
      item.copyrightYear.foreach(v => iseq = iseq :+ (copyrightYear -> JsString(v)))
      item.credentials.foreach(v => iseq = iseq :+ (credentials -> JsString(v)))
      if (!item.files.isEmpty) iseq = iseq :+ (files -> JsArray(item.files.map(Json.toJson(_))))
      if (!item.gradeLevel.isEmpty) iseq = iseq :+ (gradeLevel -> JsArray(item.gradeLevel.map(JsString(_))))
      item.itemType.foreach(v => iseq = iseq :+ (itemType -> JsString(v)))
      item.itemTypeOther.foreach(v => iseq = iseq :+ (itemTypeOther -> JsString(v)))
      if (!item.keySkills.isEmpty) iseq = iseq :+ (keySkills -> JsArray(item.keySkills.map(JsString(_))))
      item.licenseType.foreach(v => iseq = iseq :+ (licenseType -> JsString(v)))
      if (!item.primarySubject.isEmpty) iseq = iseq :+ (primarySubject -> Json.toJson(item.primarySubject))
      item.priorUse.foreach(v => iseq = iseq :+ (priorUse -> JsString(v)))
      if (!item.reviewsPassed.isEmpty) iseq = iseq :+ (reviewsPassed -> JsArray(item.reviewsPassed.map(JsString(_))))
      item.sourceUrl.foreach(v => iseq = iseq :+ (sourceUrl -> JsString(v)))
      if (!item.standards.isEmpty) iseq = iseq :+ (standards -> Json.toJson(item.standards.
        foldRight[Seq[Standard]](Seq[Standard]())((sid, acc) => Standard.findOneById(sid) match {
        case Some(standard) => acc :+ standard
        case None => Log.f("ItemWrites: no standard found given id"); acc
      })))
      item.title.foreach(v => iseq = iseq :+ (title -> JsString(v)))
      item.xmlData.foreach(v => iseq = iseq :+ (xmlData -> JsString(v)))
      JsObject(iseq)
    }
  }
  implicit object ItemReads extends Reads[Item]{
    def reads(json:JsValue):Item = {
      val item = Item("")
      item.collectionId = (json \ collectionId).asOpt[String].getOrElse("") //must do checking outside of json deserialization
      item.author = (json \ author).asOpt[String]
      item.contributor = (json \ contributor).asOpt[String]
      item.copyrightOwner = (json \ copyrightOwner).asOpt[String]
      item.copyrightYear = (json \ copyrightYear).asOpt[String]
      item.credentials = (json \ credentials).asOpt[String]
      item.files = (json \ files).asOpt[Seq[ItemFile]].getOrElse(Seq.empty[ItemFile])
      item.gradeLevel = (json \ gradeLevel).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.itemType = (json \ itemType).asOpt[String]
      item.itemTypeOther = (json \  itemTypeOther).asOpt[String]
      item.keySkills = (json \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.licenseType = (json \ licenseType).asOpt[String]
      item.primarySubject = (json \ primarySubject).asOpt[ItemSubject]
      item.priorUse = (json \ priorUse).asOpt[String]
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty)
      item.sourceUrl = (json \ sourceUrl).asOpt[String]
      try{
        item.standards = (json \ standards).asOpt[Seq[String]].map(_.map(new ObjectId(_))).getOrElse(Seq.empty)
      }catch{
        case e:IllegalArgumentException => throw new JsonValidationException(standards)
      }
      item.title = (json \ title).asOpt[String]
      item.xmlData = (json \ xmlData).asOpt[String]
      try{
        item.id = (json \ id).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())
      }catch{
        case e:IllegalArgumentException => throw new JsonValidationException(id)
      }
      queryFields.foreach(queryField =>
        if (queryField.isValueValid(queryField.key,queryField.value(item)).isLeft) throw new JsonValidationException(queryField.key)
      )
      item
    }
  }
  def updateItem(oid:ObjectId, newItem:Item):Either[InternalError,Item] = {
    val updateObj = MongoDBObject.newBuilder
    for (queryField <- queryFields){
      if (queryField.key != "_id" && queryField.key != collectionId && !(queryField.key.startsWith(standards+".")) ){
       // updateObj += (queryField.key -> queryField.value(newItem))
        queryField.value(newItem) match {
          case optField:Option[_] => if(optField.isDefined) updateObj += (queryField.key -> optField)
          case seqField:Seq[_] => if(!seqField.isEmpty) updateObj += (queryField.key -> seqField)
          case _ => updateObj += (queryField.key -> queryField.value(newItem))
        }
      }
    }
    try{
      Item.update(MongoDBObject("_id" -> oid), MongoDBObject("$set" -> updateObj.result()),false,false,Item.collection.writeConcern)
      Item.findOneById(oid) match {
        case Some(i) => Right(i)
        case None => Left(InternalError("somehow the document that was just updated could not be found",LogType.printFatal))
      }
    }catch{
      case e:SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,false,Some("error occured while updating")))
    }
  }
  val queryFields:Seq[QueryField[Item]] = Seq[QueryField[Item]](
    QueryFieldObject[Item](id,_.id, QueryField.valuefuncid),
    QueryFieldString[Item](author,_.author),
    QueryFieldString[Item](collectionId,_.collectionId),
    QueryFieldString[Item](contentType,_.contentType,_ match {
      case x:String if x == ContentType.item => Right(x)
      case _ => Left(InternalError("incorrect content type"))
    }),
    QueryFieldString[Item](contributor,_.contributor),
    QueryFieldString[Item](copyrightOwner,_.copyrightOwner),
    QueryFieldString[Item](copyrightYear,_.copyrightYear),
    QueryFieldString[Item](credentials,_.credentials, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case x:String => if(fieldValues.credentials.exists(_.key == x)) Right(x) else Left(InternalError("no valid credentials found for given value"))
        case _ => Left(InternalError("invalid value format"))
      }
    }),
    QueryFieldObjectArray[Item](files,_.files, innerQueryFields = ItemFile.queryFields),
    QueryFieldStringArray[Item](gradeLevel,_.gradeLevel, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case grades:BasicDBList =>
          if(grades.foldRight[Boolean](true)((grade,acc) => fieldValues.gradeLevels.exists(_.key == grade.toString) && acc)) Right(value)
          else Left(InternalError("gradeLevel contained invalid grade formats for values"))
        case grade:String =>
          if (fieldValues.gradeLevels.exists(_.key == grade.toString)) Right(grade) else Left(InternalError("gradeLevel contained invalid grade formats for values"))
        case _ =>
          Left(InternalError("invalid type for value in gradeLevel"))
      }
    }),
    QueryFieldString[Item](itemType,_.itemType, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case x:String => if(fieldValues.itemTypes.exists(_.key == x)) Right(value) else Left(InternalError("could not find valid item types for value"))
        case _ => Left(InternalError("invalid type for value in itemType"))
      }
    }),
    QueryFieldString[Item](itemTypeOther,_.itemTypeOther),
    QueryFieldStringArray[Item](keySkills,_.keySkills, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case skills:BasicDBList => if(skills.foldRight[Boolean](true)((skill,acc) => fieldValues.keySkills.exists(_.key == skill.toString) && acc)) Right(value)
          else Left(InternalError("key skill not found for given value"))
        case _ => Left(InternalError("invalid value type for keySkills"))
      }
    }),
    QueryFieldString[Item](licenseType,_.licenseType, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case x:String => if(fieldValues.licenseTypes.exists(_.key == x)) Right(value) else Left(InternalError("license type not found"))
        case _ => Left(InternalError("invalid value type for licenceType"))
      }
    }),
    QueryFieldObject[Item](primarySubject,_.primarySubject,innerQueryFields = ItemSubject.queryFields),
    QueryFieldStringArray[Item](priorUse,_.priorUse, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case x:String => if(fieldValues.priorUses.exists(_.key == x)) Right(value) else Left(InternalError("priorUse not found"))
        case _ => Left(InternalError("invalid value type for priorUse"))
      }
    }),
    QueryFieldStringArray[Item](reviewsPassed,_.reviewsPassed, value => {
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).
        getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      value match {
        case reviews:BasicDBList => if(reviews.foldRight[Boolean](true)((review,acc) => fieldValues.reviewsPassed.exists(_.key == review.toString) && acc)) Right(value)
          else Left(InternalError("review not found"))
        case _ => Left(InternalError("invalid value type for reviewsPassed"))
      }
    }),
    QueryFieldString[Item](sourceUrl,_.sourceUrl),
    QueryFieldObjectArray[Item](standards,_.standards, _ match {
      case x:BasicDBList => x.foldRight[Either[InternalError,Seq[ObjectId]]](Right(Seq()))((standard,acc) => {
        acc match {
          case Right(ids) => standard match {
            case x:String => try{
              Right(ids :+ new ObjectId(x))
            }catch{
              case e:IllegalArgumentException => Left(InternalError("invalid object id format for standards"))
            }
          }
          case Left(e) => Left(e)
        }
      })
      case x:String => try{
        Right(new ObjectId(x))
      }catch{
        case e:IllegalArgumentException => Left(InternalError("invalid object id format for standards"))
      }
      case _ => Left(InternalError("uknown value type for standards"))
    },Standard.queryFields),
    QueryFieldString[Item](title,_.title),
    QueryFieldString[Item](xmlData,_.xmlData)

  )
  override def preParse(dbo:DBObject):QueryParser = {
    val qp = QueryParser.buildQuery(dbo,QueryParser(),Seq(queryFields.find(_.key == standards).get))
   qp.result match {
      case Right(query) =>
        val dbquery = query.result()
        val builder = MongoDBObject.newBuilder
        if(!dbquery.isEmpty){
          val c = Standard.find(query.result(),MongoDBObject("_id" -> 1))
          val builderList = MongoDBList.newBuilder
          if (!c.isEmpty){
            c.foreach(builderList += _.id)
            builder += (standards -> MongoDBObject("$in" -> builderList.result()))
          }
        }
        removeKeys(dbo,Standard.queryFields.foldRight[Seq[String]](Seq(standards))((qf,acc) => acc :+ standards+"."+qf.key))
        QueryParser(Right(builder))
      case Left(e) => QueryParser(Left(e))
    }
  }
  private def removeKeys(dbo:DBObject,keys:Seq[String]){
    keys.foreach(key => dbo.remove(key))
    dbo.iterator.foreach(field => {
      field._2 match {
        case dblist:BasicDBList => dblist.foreach(value => value match {
          case innerdbo:BasicDBObject => removeKeys(innerdbo,keys)
          case _ => Log.f("invalid query")
        })
        case innerdbo:BasicDBObject => removeKeys(innerdbo,keys)
        case _ => Log.f("invalid query")
      }
    })
  }
}
