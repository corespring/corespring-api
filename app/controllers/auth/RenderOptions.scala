package controllers.auth

import encryption.AESCrypto
import models.auth.ApiClient
import play.api.libs.json._
import models.itemSession.ItemSession
import org.bson.types.ObjectId
import models.quiz.basic.Quiz

case class RenderOptions(
                          itemId: String = "*",
                          sessionId: String = "*",
                          assessmentId: String = "*",
                          collectionId : String = "*",
                          role: String = "student",
                          expires: Long, mode: String) {

  /**
   * if sessionId is a wildcard, the requested session must either belong to the given item or the given assessmentId
   * (if itemId is a wildcard). if both are a wildcard, then return true
   * @param id
   * @return
   */
  def allowSessionId(id:String) : Boolean = if (sessionId == RenderOptions.*){
    if (itemId != RenderOptions.*){
      try{
        ItemSession.findItemSessions(new ObjectId(itemId)).exists(_.id.toString == sessionId)
      } catch {
        case e:IllegalArgumentException => false
      }
    }else if (assessmentId != RenderOptions.*){
      try{
        Quiz.findOneById(new ObjectId(assessmentId)) match {
          case Some(quiz) => {
            quiz.questions.exists(question => {
              ItemSession.findItemSessions(question.itemId).exists(session => {
                session.id.toString == id
              })
            })
          }
          case None => false
        }
      } catch {
        case e:IllegalArgumentException => false
      }
    } else true
  } else id == sessionId

  /**
   * if itemId is a wildcard, the requested session must belong to the given assessment if assignmentId is not a wildcard
   * @param id
   * @return
   */
  def allowItemId(id: String): Boolean = if(itemId == RenderOptions.*){
    if(assessmentId == RenderOptions.*) true
    else try{
      Quiz.findOneById(new ObjectId(assessmentId)) match {
        case Some(quiz) => quiz.questions.exists(_.itemId.toString == id)
        case None => false
      }
    } catch {
      case e:IllegalArgumentException => false
    }
  } else id == itemId

  def allowAssessmentId(id: String): Boolean = allow(id,assessmentId)
  def allowCollectionId(id:String) : Boolean = allow(id, collectionId)

  def allowMode(m:String) : Boolean = allow(m,mode)

  private def allow(id:String, optionId:String) = if(optionId == RenderOptions.*) true else id == optionId

}

object RenderOptions {

  val * = "*"

  val ANYTHING = RenderOptions(expires = 0, mode = *)

  implicit object RCReads extends Reads[RenderOptions] {
    def reads(json: JsValue): RenderOptions = {
      RenderOptions(
        (json \ "itemId").asOpt[String].getOrElse(*),
        (json \ "sessionId").asOpt[String].getOrElse(*),
        (json \ "assessmentId").asOpt[String].getOrElse(*),
        (json \ "collectionId").asOpt[String].getOrElse(*),
        (json \ "role").asOpt[String].getOrElse("student"),
        (json \ "expires").as[Long],
        (json \ "mode").as[String]
      )
    }
  }

  implicit object RCWrites extends Writes[RenderOptions] {
    def writes(ro: RenderOptions): JsValue = {
      JsObject(Seq(
        "itemId" -> JsString(ro.itemId),
        "sessionId" -> JsString(ro.sessionId),
        "assessmentId" -> JsString(ro.assessmentId),
        "collectionId" -> JsString(ro.collectionId),
        "role" -> JsString(ro.role),
        "expires" -> JsNumber(ro.expires),
        "mode" -> JsString(ro.mode)
      ))
    }
  }

  def decryptOptions(apiClient: ApiClient, encrypted: String): RenderOptions = {
    val decrypted = AESCrypto.decrypt(encrypted, apiClient.clientSecret)
    Json.fromJson[RenderOptions](Json.parse(decrypted))
  }
}
