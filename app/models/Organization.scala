package models

import org.bson.types.ObjectId
import play.api.Play.current
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._
import play.api.libs.json._
import play.api.libs.json.JsObject

/**
 * An Organization
 */
case class Organization(
                         id: ObjectId,
                         name: String,
                         parentId: Option[ObjectId] = None,
                         children: Seq[ObjectId] = Seq.empty
                       )

object Organization extends ModelCompanion[Organization, ObjectId] {
  val collection = mongoCollection("organizations")
  val dao = new SalatDAO[Organization, ObjectId]( collection = collection ) {}
  val queryFields = Map("name" -> "String")

  /**
   * Returns the organizations visible to the organization specified
   *
   * @param id an organization id
   * @return
   */
  def findAllFor(id: ObjectId):SalatMongoCursor[Organization] = {
    //todo: filter results according to what is visible under the passed ID
   findAll()
  }

  //
  implicit object OrganizationWrites extends Writes[Organization] {
    def writes(org: Organization) = {
      JsObject(
        List(
          "id" -> JsString(org.id.toString),
          "name" -> JsString(org.name),
          "parentId" -> JsString(org.parentId.map(_.toString).getOrElse("")),
          "children" -> JsArray(org.children.map( c => JsString(c.toString) ).toSeq)
        )
      )
    }
  }
}



