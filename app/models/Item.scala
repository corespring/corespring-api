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

case class ItemFile(var filename:String)
object ItemFile{
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
}

case class ItemSubject(var subject:String, var category:String, var refId:String)
object ItemSubject{
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
object Item extends ModelCompanion[Item, ObjectId] with Queryable{
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
      if (!item.standards.isEmpty) iseq = iseq :+ (standards -> Json.toJson(item.standards.map(_.toString)))
      item.title.foreach(v => iseq = iseq :+ (title -> JsString(v)))
      item.xmlData.foreach(v => iseq = iseq :+ (xmlData -> JsString(v)))
      JsObject(iseq)
    }
  }
  implicit object ItemReads extends Reads[Item]{
    def reads(json:JsValue):Item = {
      val item = Item("")
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
      item.collectionId = (json \ collectionId).asOpt[String].getOrElse("") //must do checking outside of json deserialization
      item.author = (json \ author).asOpt[String]
      item.contributor = (json \ contributor).asOpt[String]
      item.copyrightOwner = (json \ copyrightOwner).asOpt[String]
      item.copyrightYear = (json \ copyrightYear).asOpt[String]
      item.credentials = (json \ credentials).asOpt[String].
        map(v => if (fieldValues.credentials.exists(_.key == v)) v else throw new JsonValidationException(credentials))
      item.files = (json \ files).asOpt[Seq[ItemFile]].getOrElse(Seq.empty[ItemFile])
      item.gradeLevel = (json \ gradeLevel).asOpt[Seq[String]].
        map(v => if(v.foldRight[Boolean](true)((g,acc) => fieldValues.gradeLevels.exists(_.key == g) && acc)) v else throw new JsonValidationException(gradeLevel)).getOrElse(Seq.empty)
      item.itemType = (json \ itemType).asOpt[String].
        map(v => if (fieldValues.itemTypes.exists(_.key == v)) v else throw new JsonValidationException(itemType))
      item.itemTypeOther = (json \  itemTypeOther).asOpt[String]
      item.keySkills = (json \ keySkills).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((s,acc) => fieldValues.keySkills.exists(_.key == s) && acc)) v else throw new JsonValidationException(keySkills)).getOrElse(Seq.empty)
      item.licenseType = (json \ licenseType).asOpt[String].
        map(v => if (fieldValues.licenseTypes.exists(_.key == v)) v else throw new JsonValidationException(licenseType))
      item.primarySubject = (json \ primarySubject).asOpt[ItemSubject]
      item.priorUse = (json \ priorUse).asOpt[String].
        map(v => if (fieldValues.priorUses.exists(_.key == v)) v else throw new JsonValidationException(priorUse))
      item.reviewsPassed = (json \ reviewsPassed).asOpt[Seq[String]].
        map(v => if (v.foldRight[Boolean](true)((r,acc) => fieldValues.reviewsPassed.exists(_.key == r) && acc)) v else throw new JsonValidationException(reviewsPassed)).getOrElse(Seq.empty)
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
      item
    }
  }
  def updateItem(oid:ObjectId, newItem:Item):Either[InternalError,Item] = {
    val updateObj = MongoDBObject.newBuilder
    for (queryField <- queryFields){
      if (queryField.key != id && queryField.key != collectionId){
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
  val queryFields:Seq[QueryField[Item]] = Seq(
    QueryField(id,QueryField.ObjectIdType,_.id),
    QueryField(author,QueryField.StringType,_.author),
    QueryField(collectionId,QueryField.StringType,_.collectionId),
    QueryField(contentType,QueryField.StringType,_.contentType),
    QueryField(contributor,QueryField.StringType,_.contributor),
    QueryField(copyrightOwner,QueryField.StringType,_.copyrightOwner),
    QueryField(copyrightYear,QueryField.NumberType,_.copyrightYear),
    QueryField(credentials,QueryField.StringType,_.credentials),
    QueryField(files,QueryField.ObjectArrayType,_.files),
    QueryField(gradeLevel,QueryField.StringArrayType,_.gradeLevel),
    QueryField(itemType,QueryField.StringType,_.itemType),
    QueryField(itemTypeOther,QueryField.StringType,_.itemTypeOther),
    QueryField(keySkills,QueryField.StringArrayType,_.keySkills),
    QueryField(licenseType,QueryField.StringType,_.licenseType),
    QueryField(primarySubject,QueryField.ObjectArrayType,_.primarySubject),
    QueryField(priorUse,QueryField.ObjectArrayType,_.priorUse),
    QueryField(reviewsPassed,QueryField.StringArrayType,_.reviewsPassed),
    QueryField(sourceUrl,QueryField.StringType,_.sourceUrl),
    QueryField(standards,QueryField.ObjectArrayType,_.standards),
    QueryField(title,QueryField.StringType,_.title),
    QueryField(xmlData,QueryField.StringType,_.xmlData)

  )
  def parseQuery(query:String):Either[InternalError, DBObject] = {
    JSON.parse(query) match {
      case dbo:DBObject => try{
        var queryBuilder = MongoDBObject.newBuilder
        val fields:Iterator[(String,AnyRef)] = dbo.iterator
        Right(queryBuilder.result())
      }catch{
        case e:RuntimeException => Left(InternalError(e.getMessage,LogType.printError,true))
      }
      case _ => Left(InternalError("invalid format for query. could not parse into db object",LogType.printError,true))
    }
  }
  def parseFields(iter:Iterator[(String,AnyRef)], acc:Either[InternalError,mutable.Builder[(String,Any),DBObject]]):Either[InternalError,mutable.Builder[(String,Any),DBObject]] = {
    if (iter.hasNext && acc.isRight){
      val field = iter.next()

    }else acc
    acc
  }
  def parseOuterField(field:(String,AnyRef), acc:mutable.Builder[(String,Any),DBObject]):Either[InternalError,mutable.Builder[(String,Any),DBObject]] = {

    field._1 match {
      case x if x startsWith files+"." => field._1.substring(field._1.indexOf(".")+1) match {
        case x if x endsWith ItemFile.filename => QueryParser.parseValue(field._1,QueryField.StringType,field._2,acc)
        case _ => Left(InternalError("invalid attribute for embedded object: "+field._1,LogType.printError,true))
      }
      case x if x startsWith primarySubject+"." => field._1.substring(field._1.indexOf(".")+1) match {
        case ItemSubject.category => QueryParser.parseValue(field._1,QueryField.StringType,field._2,acc)
        case ItemSubject.refId => QueryParser.parseValue(field._1,QueryField.StringType,field._2,acc)
        case ItemSubject.subject => QueryParser.parseValue(field._1,QueryField.StringType,field._2,acc)
        case _ => Left(InternalError("invalid attribute for embedded object: "+field._1,LogType.printError,true))
      }
      case `contentType` => field._2 match {
        case ContentType.item => Right(acc)
        case _ => Left(InternalError("invalid content type",LogType.printError,true))
      }
      case `standards` => Right(acc) //todo finish this part
      case _ => queryFields.find(_.key == field._1) match {
        case Some(queryField) => QueryParser.parseValue(queryField.key,queryField.keyType,field._2,acc)
        case None => Left(InternalError("unknown field type: "+field._1,LogType.printError,true))
      }
    }
  }
}
