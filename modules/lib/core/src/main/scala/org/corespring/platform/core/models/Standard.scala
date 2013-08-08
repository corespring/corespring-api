package org.corespring.platform.core.models

import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.models.search.Searchable


case class Standard(var dotNotation: Option[String] = None,
                     var guid:Option[String] = None,
                     var subject: Option[String] = None,
                     var category: Option[String] = None,
                     var subCategory: Option[String] = None,
                     var standard: Option[String] = None,
                     var id: ObjectId = new ObjectId()
                     )

object Standard extends ModelCompanion[Standard,ObjectId] with Searchable{

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

  //Ensure dotNotation is unique
  collection.ensureIndex(DotNotation)

  //import org.corespring.platform.core.models.json._
  //implicit val reads = Json.reads[Standard]
  //implicit val writes = Json.writes[Standard]


  implicit object StandardWrites extends Writes[Standard] {

    def writes(obj: Standard) = {
      JsObject(
        List(
          Id -> JsString(obj.id.toString),
          DotNotation -> JsString(obj.dotNotation.getOrElse("")),
          Subject -> JsString(obj.subject.getOrElse("")),
          Category -> JsString(obj.category.getOrElse("")),
          SubCategory -> JsString(obj.subCategory.getOrElse("")),
          Standard -> JsString(obj.standard.getOrElse(""))
        )
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
    guid
  )

  def findOneByDotNotation(dn:String) : Option[Standard] = findOne(MongoDBObject(DotNotation -> dn))

  /** validate that the dotNotation exists
   * @param dn
   * @return true if its valid false if not
   */
  def isValidDotNotation(dn:String) : Boolean = findOne(MongoDBObject(DotNotation -> dn)) match {
    case Some(s) => true
    case _ => false
  }
}

