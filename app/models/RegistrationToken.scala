package models

import com.novus.salat.dao.ModelCompanion
import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._
import controllers.QueryParser
import org.joda.time.DateTime


case class RegistrationToken(var uuid: String = "",
                 var email: String = "",
                 var creationTime: Option[DateTime] = None,
                 var expirationTime: Option[DateTime] = None,
                 var id: ObjectId = new ObjectId()) extends Identifiable

object RegistrationToken extends DBQueryable[RegistrationToken] {

  val collection = mongoCollection("regtokens")
  val dao = new SalatDAO[RegistrationToken, ObjectId](collection = collection) {}

  val Id = "id"
  val Uuid = "uuid"
  val Email = "email"
  val Created = "creationTime"
  val Expires = "expirationTime"

  implicit object RegistrationTokenWrites extends Writes[RegistrationToken] {
    def writes(token: RegistrationToken) = {
      var list = List[(String,JsValue)]()
      if ( token.uuid.nonEmpty ) list = (Uuid -> JsString(token.uuid)) :: list
      if ( token.email.nonEmpty ) list = (Email -> JsString(token.email)) :: list
      if ( token.creationTime.isDefined ) list = (Created -> JsNumber(token.creationTime.get.getMillis)) :: list
      if ( token.expirationTime.isDefined ) list = (Expires -> JsNumber(token.expirationTime.get.getMillis)) :: list
      list =  "id" -> JsString(token.id.toString) :: list
      JsObject(list)
    }
  }

  implicit object RegistrationTokenReads extends Reads[RegistrationToken] {
      def reads(json: JsValue): RegistrationToken = {
        RegistrationToken(
          uuid = (json \ Uuid).asOpt[String].getOrElse(""),
          email = (json \ Email).asOpt[String].getOrElse(""),
          creationTime = (json \ Created).asOpt[Long].map(new DateTime(_)),
          expirationTime = (json \ Expires).asOpt[Long].map(new DateTime(_))
        )
      }
    }

  val queryFields: Seq[QueryField[RegistrationToken]] = Seq(
    QueryFieldString[RegistrationToken](Uuid, _.uuid),
    QueryFieldString[RegistrationToken](Email, _.email),
    QueryFieldString[RegistrationToken](Created, _.creationTime),
    QueryFieldString[RegistrationToken](Expires, _.expirationTime),
    QueryFieldObject[RegistrationToken](Id, _.id, QueryField.valuefuncid)
  )

  val description = "Tokens"
}
