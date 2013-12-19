package org.corespring.clientlogging

import java.util.Date
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc._

trait SimpleLogger {
  def log(logType : String, msg : String, stacktrace : String) : Unit
}

case class LogDetails(msg: String, stacktrace: String)

object LogDetails {
  def apply(r: Request[AnyContent]): Option[LogDetails] = {
    for {
      json <- r.body.asJson
      msg <- (json \ "message").asOpt[String]
      stacktrace <- (json \ "stacktrace").asOpt[String].orElse(Some(""))
    } yield LogDetails(msg, stacktrace)

  }
}


object ClientLogger extends Controller{

  val logger: SimpleLogger = new SimpleLogger {

    private lazy val internalLogger = LoggerFactory.getLogger("client-logger")

    def log(logType: String, m: String, stacktrace: String): Unit = {

      val end: Date = new Date()
      def logOut =
        s"""
          |***Client Log Entry***
          |$logType: $m
          |$stacktrace"
          |***End ${end.toString}***
        """.stripMargin

      internalLogger.info(logOut)
    }
  }


  val logTypes: Seq[String] = Seq("info", "fatal", "error", "warn", "debug", "info")

  def submitLog(logType: String) = Action {
    request =>

      def log: Result = {
        LogDetails(request).map {
          d => logger.log(logType, d.msg, d.stacktrace)
        }
        Ok
      }

      logType match {
        case t: String if logTypes.contains(t) => log
        case _ => BadRequest(Json.toJson(Json.obj("error" -> JsString(s"unknown log type: $logType"))))
      }
  }
}
