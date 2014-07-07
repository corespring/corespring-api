package org.corespring.platform.core.models

import play.api.Play.current
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.models.search.Searchable
import play.api.cache.Cache
import com.mongodb.casbah.commons.MongoDBObject
import scala.collection.immutable.{ListMap, SortedMap}

case class Standard(var dotNotation: Option[String] = None,
  var guid: Option[String] = None,
  var subject: Option[String] = None,
  var category: Option[String] = None,
  var subCategory: Option[String] = None,
  var standard: Option[String] = None,
  var id: ObjectId = new ObjectId(),
  var grades: Seq[String] = Seq.empty[String],
  var legacyItem: Boolean = false) extends StandardGroup {

  val kAbbrev = "[K|\\d].([\\w|-]+)\\..*".r
  val abbrev = "([\\w|-]+)..*".r
  val last = ".*\\.(\\w+)$".r

  def abbreviation: Option[String] = dotNotation match {
    case Some(notation) => notation match {
      case kAbbrev(a) => Some(a)
      case abbrev(a) => Some(a)
      case _ => None
    }
    case _ => None
  }

  def code: Option[String] = dotNotation match {
    case Some(notation) => notation match {
      case last(code) => Some(code)
      case _ => None
    }
    case _ => None
  }

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

  implicit object StandardFormat extends Format[Standard] {

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
        }))
    }

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

  lazy val sorter: (String, String) => Boolean = (a,b) => {
    val standards: Seq[Standard] = cachedStandards()
    ((standards.find(_.dotNotation == Some(a)), standards.find(_.dotNotation == Some(b))) match {
      case (Some(one), Some(two)) => standardSorter(one, two)
      case _ => throw new IllegalArgumentException("Could not find standard for dot notation")
    })
  }

  lazy val groupMap = {
    cachedStandards().sortWith(standardSorter).foldLeft(ListMap.empty[String, Seq[Standard]]) { (map, standard) =>
      standard.group match {
        case Some(group) => map.get(group) match {
          case Some(standards: Seq[Standard]) => map + (group -> (standards :+ standard))
          case _ => map + (group -> Seq(standard))
        }
        case _ => map
      }
    }
  }

  def cachedStandards(): Seq[Standard] = {
    val cacheKey = "standards_sort"
    Cache.get(cacheKey) match {
      case Some(standardsJson: String) => Json.parse(standardsJson).as[Seq[Standard]]
      case _ => {
        val standards = findAll().toSeq
        Cache.set(cacheKey, Json.toJson(standards).toString)
        standards.toList
      }
    }
  }

  lazy val legacy = findAll().filter(_.legacyItem).toSeq
  lazy val standardSorter: (Standard, Standard) => Boolean = (one, two) => {
    StandardOrdering.compare(one, two) < 0
  }

  val description = "common core state standards"
  override val searchableFields = Seq(
    DotNotation,
    Subject,
    Category,
    SubCategory,
    Standard,
    guid)

  def baseQuery: DBObject = new BasicDBObject("legacyItem", new BasicDBObject("$ne", true))
  def baseQuery(mongo: DBObject): DBObject = {
    val base = baseQuery
    base.putAll(mongo.toMap)
    base
  }

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

