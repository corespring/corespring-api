package controllers

import play.api.mvc.{Request, Result, Action, Controller}
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import play.api.data.validation.ValidationError
import scalaz._
import Scalaz._
import org.corespring.common.log.ClientLogging

object ClientLogger extends Controller with ClientLogging{
  def submitLog(logType:String) = Action(parse.json) { request =>
//    val logEntryResult:JsResult[Validation[ValidationError,ClientLogEntry]] = for {
//      message <- (request.body \ "message").validate[String]
//    } yield ClientLogEntry(message,logType)
//    logEntryResult match {
//      case JsSuccess(Success(logEntry),_) => {
//        logEntry.stacktrace = (request.body \ "stacktrace").asOpt[String]
//        LogEntry.log(logEntry)
//        Ok
//      }
//      case JsSuccess(Failure(error),_) => BadRequest(JsObject(Seq(
//        "errors" -> JsArray(Seq(JsString(error.message)))
//      )))
//      case JsError(errors) => BadRequest(JsObject(Seq(
//        "errors" -> JsArray(errors.flatMap(_._2).map(e => JsString(e.message)).seq)
//      )))
//    }
    Ok
  }
}
