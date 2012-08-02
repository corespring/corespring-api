package models

import org.bson.types.ObjectId
import play.api.Play.current
import mongoContext._
import com.novus.salat.dao.{SalatMongoCursor, SalatDAO, ModelCompanion}
import se.radley.plugin.salat._
import play.api.libs.json.{JsArray, JsString, JsObject, Writes}

/**
 * A ContentCollection
 */
case class ContentCollection(
                       id: ObjectId,
                       name: String,
                       organizations: Seq[ObjectId] = Seq.empty
                     )

object ContentCollection extends ModelCompanion[ContentCollection, ObjectId] {
  val collection = mongoCollection("collections")
  val dao = new SalatDAO[ContentCollection, ObjectId]( collection = collection ) {}

  /**
   * Returns the collections attached to the organizations the caller has access to
   *
   * @param id an organization id
   * @return
   */
  def findAllFor(id: ObjectId):SalatMongoCursor[ContentCollection] = {
    //todo: filter results according to what is visible under the passed ID
    findAll()
  }

  implicit object CollectionWrites extends Writes[ContentCollection] {
    def writes(coll:ContentCollection) = {
      JsObject(
        List(
          "id" -> JsString(coll.id.toString),
          "name" -> JsString(coll.name),
          "organizations" -> JsArray(coll.organizations.map( o => JsString(o.toString) ).toSeq)
        )
      )
    }
  }
}
