package org.corespring.clientlogging

import play.api.libs.json.JsSuccess
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

object ClientLogger extends Controller with ClientLogging{
  def submitLog(logType:String) = Action(parse.json) { request =>
    (request.body \ "message").validate[String] match {
      case JsSuccess(message,_) => {
        val stacktrace = (request.body \ "stacktrace").asOpt[String]
        logType match {
          case "debug" => logger.debug(message,stacktrace.getOrElse("")); Ok
          case "info" =>  logger.info(message,stacktrace.getOrElse("")); Ok
          case "warn" => logger.warn(message,stacktrace.getOrElse("")); Ok
          case "error" => logger.error(message,stacktrace.getOrElse("")); Ok
          case "fatal" => logger.fatal(message,stacktrace.getOrElse("")); Ok
          case _ => BadRequest(s"received message with no type: $message")
        }
      }
      case JsError(errors) => BadRequest(JsObject(Seq(
        "errors" -> JsArray(errors.flatMap(_._2).map(e => JsString(e.message)).seq)
      )))
    }
  }
}
