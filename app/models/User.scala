package models

import org.bson.types.ObjectId
import play.api.Play.current
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._
import play.api.libs.json._
import play.api.libs.json.JsObject


/**
 * A User
 */
case class User(
                 id: ObjectId,
                 userName: String,
                 fullName: String,
                 email: String
               )

object User extends ModelCompanion[User, ObjectId] {
  val collection = mongoCollection("users")
  val dao = new SalatDAO[User, ObjectId]( collection = collection ) {}

  /**
   * Returns the users visible to the organization specified
   *
   * @param id an organization id
   * @return
   */
  def findAllFor(id: ObjectId):SalatMongoCursor[User] = {
    //todo: filter results according to what is visible under the passed ID
    findAll()
  }

  //
  implicit object UserWrites extends Writes[User] {
    def writes(user: User) = {
      JsObject(
        List(
          "id" -> JsString(user.id.toString),
          "userName" -> JsString(user.userName),
          "fullName" -> JsString(user.fullName),
          "email" -> JsString(user.email)
        )
      )
    }
  }
}



