package org.corespring.platform.core.models

import play.api.Play.current
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.models.search.Searchable

case class Standard(var dotNotation: Option[String] = None,
  var guid: Option[String] = None,
  var subject: Option[String] = None,
  var category: Option[String] = None,
  var subCategory: Option[String] = None,
  var standard: Option[String] = None,
  var id: ObjectId = new ObjectId(),
  var grades: Seq[String] = Seq.empty[String]){

  val kAbbrev = "K.(\\w+)\\..*".r
  val abbrev = "(\\w+)\\..*".r

  def abbreviation = dotNotation.map(_ match {
    case kAbbrev(a) => Some(a)
    case abbrev(a) => Some(a)
    case _ => None
  })
}

object Standard extends ModelCompanion[Standard, ObjectId] with Searchable with JsonUtil {

  val collection = mongoCollection("ccstandards")

  import org.corespring.platform.core.models.mongoContext.context
  val dao = new SalatDAO[Standard, ObjectId](collection = collection) {}

  val Id = "id"
  val DotNotation = "dotNotation"
  val Subject = "subject"
  val Category = "category"
  val SubCategory = "subCategory"
  val Standard = "standard"
  val guid = "guid"
  val grades = "grades"

  //Ensure dotNotation is unique
  collection.ensureIndex(DotNotation)

  implicit object StandardWrites extends Writes[Standard] {

    def writes(obj: Standard) = {
      partialObj(
        Id -> Some(JsString(obj.id.toString)),
        DotNotation -> Some(JsString(obj.dotNotation.getOrElse(""))),
        Subject -> Some(JsString(obj.subject.getOrElse(""))),
        Category -> Some(JsString(obj.category.getOrElse(""))),
        SubCategory -> Some(JsString(obj.subCategory.getOrElse(""))),
        Standard -> Some(JsString(obj.standard.getOrElse(""))),
        grades -> (obj.grades match {
          case nonEmpty if grades.nonEmpty => Some(JsArray(obj.grades.map(JsString(_))))
          case _ => None
        })
      )
    }
  }

  implicit object StandardReads extends Reads[Standard] {
    def reads(json: JsValue) = {
      val standard = new Standard()
      standard.dotNotation = (json \ DotNotation).asOpt[String]
      standard.guid = (json \ guid).asOpt[String]
      standard.subject = (json \ Subject).asOpt[String]
      standard.category = (json \ Category).asOpt[String]
      standard.subCategory = (json \ SubCategory).asOpt[String]
      standard.standard = (json \ Standard).asOpt[String]
      standard.grades = (json \ grades).as[Seq[String]]
      JsSuccess(standard)
    }
  }
  val description = "common core state standards"
  override val searchableFields = Seq(
    DotNotation,
    Subject,
    Category,
    SubCategory,
    Standard,
    guid)

  def findOneByDotNotation(dn: String): Option[Standard] = findOne(MongoDBObject(DotNotation -> dn))

  /**
   * validate that the dotNotation exists
   * @param dn
   * @return true if its valid false if not
   */
  def isValidDotNotation(dn: String): Boolean = findOne(MongoDBObject(DotNotation -> dn)) match {
    case Some(s) => true
    case _ => false
  }
}

