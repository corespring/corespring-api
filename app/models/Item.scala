package models

import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.mongodb.casbah.Imports._
import controllers.{LogType, InternalError}
import collection.mutable
import scala.Either
import mongoContext._

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

case class Item( var collectionId:String,
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
                var id:ObjectId = new ObjectId())// extends Content
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
      iseq = iseq :+ (contentType -> JsString(item.contentType))
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
      val item = Item((json \ collectionId).as[String],
        (json \ contentType).as[String] match {
          case ContentType.item => ContentType.item
          //case ContentType.assessment => ContentType.assessment
          //case ContentType.materials => ContentType.materials
          case _ => throw new JsonValidationException(contentType)
        })
      val fieldValues:FieldValue = FieldValue.findOne(MongoDBObject(FieldValue.Version -> FieldValuesVersion)).getOrElse(throw new RuntimeException("could not find field values doc with specified version"))
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
  val queryFields = Map(
      id -> "String",
      author -> "String",
      collectionId -> "ObjectId",
      contentType -> "String",
      contributor -> "String",
      copyrightOwner -> "String",
      copyrightYear -> "Int",
      credentials -> "String",
      files -> "Seq[String]",
      gradeLevel -> "Seq[String]",
      itemType -> "String",
      itemTypeOther -> "String",
      keySkills -> "Seq[String]",
      licenseType -> "String",
      primarySubject -> "Map[String, String]",
      priorUse -> "String",
      reviewsPassed -> "Seq[String]",
      sourceUrl -> "String",
      standards -> "Seq[String]",
      title -> "String",
      xmlData -> "String"
  )
  def parseQuery(json:JsValue):Either[InternalError, DBObject] = {
    var queryBuilder = MongoDBObject.newBuilder
    try{
      val fields:Seq[(String,JsValue)] = json.as[JsObject].fields
      Right(queryBuilder.result())
    }catch{
      case e:RuntimeException => Left(InternalError(e.getMessage,LogType.printError,true))
    }
  }
  def parseFields(iter:Iterator[(String,JsValue)], acc:Either[InternalError,mutable.Builder[(String,Any),DBObject]]):Either[InternalError,mutable.Builder[(String,Any),DBObject]] = {
    if (iter.hasNext && acc.isRight){
      val field = iter.next()

    }else acc
    acc
  }
  def parseOuterField(field:(String,JsValue), acc:mutable.Builder[(String,Any),DBObject]):Either[InternalError,mutable.Builder[(String,Any),DBObject]] = {
    field._1 match {
      case `id` => field._2 match {
        case jsobj:JsObject =>
      }
      case `author` =>
      case `collectionId` =>
      case `contentType` =>
      case `contributor` =>
      case `copyrightOwner` =>
      case `copyrightYear` =>
      case `credentials` =>
      case `files` =>
      case `gradeLevel` =>
      case `itemType` =>
      case `itemTypeOther` =>
      case `keySkills` =>
      case `licenseType` =>
      case `primarySubject` =>
      case `priorUse` =>
      case `reviewsPassed` =>
      case `sourceUrl` =>
      case `standards` =>
      case `title` =>
      case `xmlData` =>
    }
    Right(acc)
  }
  def parseEmbedded(key:String, keyType:String, embedded:JsObject, acc:mutable.Builder[(String,Any),DBObject]):Either[InternalError, mutable.Builder[(String,Any),DBObject]] = {
    if (embedded.fields.size == 1){
      val field = embedded.fields(0)
      keyType match {
        case "ObjectId" => field._1 match {
          case "$ne" => field._2 match {
            case x:JsString => try{
              acc += (key -> MongoDBObject("$ne" -> new ObjectId(x.as[String])))
              Right(acc)
            } catch{
              case e:IllegalArgumentException => Left(InternalError("{"+key+":{$ne:"+x+"}} : "+x+"value is not an object id",LogType.printError,true))
            }
            case _ => Left(InternalError("{"+key+":{$ne:"+field._2.toString()+"}} : "+field._2.toString()+"value is not a string",LogType.printError,true))
          }
          case "$in" => field._2 match {
            case x:JsArray => //todo x.value.map(ids )
          }
          case "$nin" =>
        }
        case "String" => field._1 match {
          case "$ne" => field._2 match {
            case x:JsString =>
              acc += (key -> MongoDBObject("$ne" -> x.as[String]))
              Right(acc)
            case _ => Left(InternalError("{"+key+":{$ne:"+field._2.toString()+"}} : "+field._2.toString()+"value is not a string",LogType.printError,true))
          }
        }
        case "Number" =>
        case "Seq[String]" =>
        case "Seq[Number]" =>
        case "Seq[Object]" =>
        case _ => throw new RuntimeException("you passed an unknown key type jackass")
      }
    }else Left(InternalError(key+" contained multiple values",LogType.printError,true))
    Left(InternalError("blerg",LogType.printError))
  }
  case class JsonValidationException(field:String) extends RuntimeException("invalid value for: "+field)
}
