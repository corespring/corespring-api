package models

import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._

case class ItemFile(var filename:String)
object ItemFile{
  val filename = "filename"
  implicit object ItemFileWrites extends Writes[ItemFile]{
    def writes(itemFile:ItemFile) = {
      JsObject(Seq[(String,JsValue)](filename -> JsString(itemFile.filename)))
    }
  }
}


case class Item( var collectionId:Option[String] = None,
                 var contentType:Option[String] = None,
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
                var primarySubject:Map[String,String] = Map(),     //TODO: define primary subject as an object instead of map
                var priorUse:Option[String] = None,
                var reviewsPassed:Seq[String] = Seq(),
                var sourceUrl:Option[String] = None,
                var standards:Seq[Standard] = Seq(),
                var title:Option[String] = None,
                var xmlData:Option[String] = None,
                var id:ObjectId = new ObjectId()) extends Content
/**
 * An Item model
 */
object Item extends ModelCompanion[Item, ObjectId] {

  val collection = Content.collection
  val dao = new SalatDAO[Item, ObjectId](collection = collection) {}

  val id = "Id"
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
      item.collectionId.foreach(v => iseq = iseq :+ (collectionId -> JsString(v)))
      item.contentType.foreach(v => iseq = iseq :+ (contentType -> JsString(v)))
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
      if (!item.primarySubject.isEmpty) iseq = iseq :+ (primarySubject -> JsObject(item.primarySubject.toSeq.map(ps => (ps._1,JsString(ps._2)))))
      item.priorUse.foreach(v => iseq = iseq :+ (priorUse -> JsString(v)))
      if (!item.reviewsPassed.isEmpty) iseq = iseq :+ (reviewsPassed -> JsArray(item.reviewsPassed.map(JsString(_))))
      item.sourceUrl.foreach(v => iseq = iseq :+ (sourceUrl -> JsString(v)))
      if (!item.standards.isEmpty) iseq = iseq :+ (standards -> Json.toJson(item.standards))
      item.title.foreach(v => iseq = iseq :+ (title -> JsString(v)))
      item.xmlData.foreach(v => iseq = iseq :+ (xmlData -> JsString(v)))
      JsObject(iseq)
    }
  }
  implicit object ItemReads extends Reads[Item]{
    def reads(json:JsValue):Item = {
      val item = Item()
      item.author = (json \ author).asOpt[String]
      item.collectionId = (json \ collectionId).asOpt[String]
      //item.contentType = (json \ contentType).asOpt[String].getOrElse()
       // filter(v => v == ContentType.item || v == ContentType.assessment || v == ContentType.materials).getOrElse(throw new JsonValidationException(contentType))
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
  case class JsonValidationException(field:String) extends RuntimeException("invalid field for: "+field)
}
