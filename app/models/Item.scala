package models

import se.radley.plugin.salat
import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString

case class Item(var author:Option[String] = None,
                var collectionId:Option[String] = None,
                var contentType:Option[String] = None,
                var contributor:Option[String] = None,
                var copyrightOwner:Option[String] = None,
                var copyrightYear:Option[String] = None,
                var credentials:Option[String] = None,
                var files:Option[Seq[String]] = None,
                var gradeLevel:Option[Seq[String]] = None,
                var itemType:Option[String] = None,
                var itemTypeOther:Option[String] = None,
                var keySkills:Option[Seq[String]] = None,
                var licenseType:Option[String] = None,
                var primarySubject:Option[Map[String,String]] = None,
                var priorUse:Option[String] = None,
                var reviewsPassed:Option[Seq[String]] = None,
                var sourceUrl:Option[String] = None,
                var standards:Option[Seq[String]] = None,
                var title:Option[String] = None,
                var xmlData:Option[String] = None,
                var id:ObjectId = new ObjectId()) extends Content
/**
 * An Item model
 */
object Item {
  val collection = Content.collection

  val Author = "author"
  val CollectionId = Content.collectionId
  val ContentType = Content.contentType
  val Contributor = "contributor"
  val CopyrightOwner = "copyrightOwner"
  val CopyrightYear = "copyrightYear"
  val Credentials = "credentials"
  val Files = "files"
  val GradeLevel = "gradeLevel"
  val ItemType = "itemType"
  val ItemTypeOther = "itemTypeOther"
  val KeySkills = "keySkills"
  val LicenseType = "licenseType"
  val PrimarySubject = "primarySubject"
  val PriorUse = "priorUse"
  val ReviewsPassed = "reviewsPassed"
  val SourceUrl = "sourceUrl"
  val Standards = "standards"
  val Title = "title"
  val XmlData = "xmlData"

  implicit object ItemWrites extends Writes[Item] {
    def writes(item: Item) = {
      var iseq:Seq[(String,JsValue)] = Seq("id" -> JsString(item.id.toString))
      item.author.foreach(v => iseq = iseq :+ (Author -> JsString(v)))
      item.collectionId.foreach(v => iseq = iseq :+ (CollectionId -> JsString(v)))
      item.contentType.foreach(v => iseq = iseq :+ (ContentType -> JsString(v)))
      item.contributor.foreach(v => iseq = iseq :+ (Contributor -> JsString(v)))
      item.copyrightOwner.foreach(v => iseq = iseq :+ (CopyrightOwner -> JsString(v)))
      item.copyrightYear.foreach(v => iseq = iseq :+ (CopyrightYear -> JsString(v)))
      item.credentials.foreach(v => iseq = iseq :+ (Credentials -> JsString(v)))
      item.files.foreach(v => iseq = iseq :+ (Files -> JsArray(v.map(JsString(_)))))
      item.gradeLevel.foreach(v => iseq = iseq :+ (GradeLevel -> JsArray(v.map(JsString(_)))))
      item.itemType.foreach(v => iseq = iseq :+ (ItemType -> JsString(v)))
      item.itemTypeOther.foreach(v => iseq = iseq :+ (ItemTypeOther -> JsString(v)))
      item.keySkills.foreach(v => iseq = iseq :+ (KeySkills -> JsArray(v.map(JsString(_)))))
      item.licenseType.foreach(v => iseq = iseq :+ (LicenseType -> JsString(v)))
      item.primarySubject.foreach(v => iseq = iseq :+ (PrimarySubject -> JsObject(v.toSeq.map(ps => (ps._1,JsString(ps._2))))))
      item.priorUse.foreach(v => iseq = iseq :+ (PriorUse -> JsString(v)))
      item.reviewsPassed.foreach(v => iseq = iseq :+ (ReviewsPassed -> JsArray(v.map(JsString(_)))))
      item.sourceUrl.foreach(v => iseq = iseq :+ (SourceUrl -> JsString(v)))
      item.standards.foreach(v => iseq = iseq :+ (Standards -> JsArray(v.map(JsString(_)))))
      item.title.foreach(v => iseq = iseq :+ (Title -> JsString(v)))
      item.xmlData.foreach(v => iseq = iseq :+ (XmlData -> JsString(v)))
      JsObject(iseq)
    }
  }
  val queryFields = Map(
      Author -> "String",
      CollectionId -> "ObjectId",
      ContentType -> "String",
      Contributor -> "String",
      CopyrightOwner -> "String",
      CopyrightYear -> "Int",
      Credentials -> "String",
      Files -> "Seq[String]",
      GradeLevel -> "Seq[String]",
      ItemType -> "String",
      ItemTypeOther -> "String",
      KeySkills -> "Seq[String]",
      LicenseType -> "String",
      PrimarySubject -> "Map[String, String]",
      PriorUse -> "String",
      ReviewsPassed -> "Seq[String]",
      SourceUrl -> "String",
      Standards -> "Seq[String]",
      Title -> "String",
      XmlData -> "String"
  )
}
