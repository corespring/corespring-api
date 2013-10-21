package controllers

import play.api.mvc.{Request, Result, Action, Controller}
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import play.api.data.validation.ValidationError
import scalaz._
import Scalaz._
import org.corespring.common.log.{ClientLogEntry, ClientLogging}

object ClientLogger extends Controller with ClientLogging{
  def submitLog(logType:String) = Action(parse.json) { request =>
    (request.body \ "message").validate[String] match {
      case JsSuccess(message,_) => {
        val stacktrace = (request.body \ "stacktrace").asOpt[String]
        logType match {
          case "debug" => logger.debug(message,stacktrace.getOrElse(""))
          case "info" =>  logger.info(message,stacktrace.getOrElse(""))
          case "warn" => logger.warn(message,stacktrace.getOrElse(""))
          case "error" => logger.error(message,stacktrace.getOrElse(""))
          case "fatal" => logger.fatal(message,stacktrace.getOrElse(""))
        }
        Ok
      }
      case JsError(errors) => BadRequest(JsObject(Seq(
        "errors" -> JsArray(errors.flatMap(_._2).map(e => JsString(e.message)).seq)
      )))
    }
    Ok
  }
}
