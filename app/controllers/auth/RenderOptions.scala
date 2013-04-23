package controllers.auth

import controllers.InternalError
import encryption.AESCrypto
import models.auth.ApiClient
import play.api.libs.json._
import scala.Left
import scala.Right
import scala.Some


case class RenderOptions(itemId:Option[String] = None, sessionId:Option[String] = None, assessmentId:Option[String] = None, role:Option[String] = None, expires:Long, mode:String){

  def overwriteOptions(options:RenderOptions):Either[InternalError,RenderOptions] = {

    def overwriteValue(optvalue1:Option[String], optvalue2:Option[String],name:String):Either[InternalError,Option[String]] = optvalue1 match {
      case Some(value1) => optvalue2 match {
        case Some(value2) => if (value2 == RenderOptions.* || value1 == value2) Right(Some(value1))
        else Left(InternalError("the given value of "+value1+" will not be used for "+name+" because it overrides a value in the session",addMessageToClientOutput = true))
        case None => Left(InternalError("you may not override "+name+" when "+name+" is present in the session",addMessageToClientOutput = true))
      }
      case None => Right(optvalue2)
    }

    overwriteValue(options.itemId,itemId,"itemId") match {
      case Right(newItemId) => overwriteValue(options.assessmentId,assessmentId,"assessmentId") match {
        case Right(newAssessmentId) => overwriteValue(options.sessionId,sessionId,"sessionId") match {
          case Right(newSessionId) => overwriteValue(options.role,role,"role") match {
            case Right(newRole) => overwriteValue(Some(options.mode),Some(mode),"mode") match {
              case Right(Some(newMode)) => Right(RenderOptions(newItemId,newSessionId,newAssessmentId,newRole,expires,newMode))
              case Right(_) => throw new RuntimeException("RenderOptions.overwriteOptions: this error should never occur")
              case Left(e) => Left(e)
            }
            case Left(e) => Left(e)
          }
          case Left(e) => Left(e)
        }
        case Left(e) => Left(e)
      }
      case Left(e) => Left(e)
    }
  }
}


object RenderOptions{

  val * = "*"

  val ANYTHING = RenderOptions(Some(*),Some(*),Some(*),Some(*), 0,*)

  //  /**
  //   *
  //   * @param constraints1 the constraints in question
  //   * @param constraints2 the constraints
  //   * @return
  //   */
  //  def authCheck(constraints1:RenderOptions, constraints2:RenderOptions):Boolean = {
  //    constraints1.itemId.map(itemId1 => constraints2.itemId.exists(itemId2 => itemId2 == itemId1 || itemId2 == ANY_VALUE)).getOrElse(true) &&
  //    constraints1.sessionId.map(itemSessionId1 => constraints2.sessionId.exists(itemSessionId2 => itemSessionId2 == itemSessionId1 || itemSessionId2 == ANY_VALUE)).getOrElse(true) &&
  //    constraints1.assessmentId.map(assessmentId1 => constraints2.assessmentId.exists(assessmentId2 => assessmentId2 == assessmentId1)).getOrElse(true) &&
  //    constraints1.role == constraints2.role && constraints1.mode == constraints2.mode &&
  //    (constraints1.expires < System.currentTimeMillis() || constraints1.expires == 0)
  //  }
  implicit object RCReads extends Reads[RenderOptions]{
    def reads(json:JsValue):RenderOptions = {
      RenderOptions(
        (json \ "itemId").asOpt[String],
        (json \ "sessionId").asOpt[String],
        (json \ "assessmentId").asOpt[String],
        (json \ "role").asOpt[String],
        (json \ "expires").as[Long],
        (json \ "mode").as[String]
      )
    }
  }
  implicit object RCWrites extends Writes[RenderOptions]{
    def writes(rc:RenderOptions):JsValue = {
      JsObject(Seq(
        "itemId" -> rc.itemId.map(JsString(_)),
        "sessionId" -> rc.sessionId.map(JsString(_)),
        "assessmentId" -> rc.assessmentId.map(JsString(_)),
        "role" -> rc.role.map(JsString(_)),
        "expires" -> Some(JsNumber(rc.expires)),
        "mode" -> Some(JsString(rc.mode))
      ).filter(_._2.isDefined).map(prop => (prop._1,prop._2.get)))
    }
  }
  //  def decryptOptions(clientId:String, encrypted:String):Option[RenderOptions] = {
  //    ApiClient.findByKey(clientId) match {
  //      case Some(apiClient) => Some(decryptOptions(apiClient,encrypted))
  //      case None => None
  //    }
  //  }
  def decryptOptions(apiClient:ApiClient, encrypted:String):RenderOptions = {
    val decrypted = AESCrypto.decrypt(encrypted,apiClient.clientSecret)
    Json.fromJson[RenderOptions](Json.parse(decrypted))
  }
}
