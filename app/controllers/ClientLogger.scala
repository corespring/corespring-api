package controllers

import play.api.mvc.{Request, Result, Action, Controller}
import org.corespring.common.log.{ClassLogging, PackageLogging}
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import play.api.data.validation.ValidationError
import scalaz._
import Scalaz._

object MessageType extends Enumeration{
  type MessageType = Value
  val Fatal, Error, Warn, Info, Debug = Value
  def toType(messageType:String):Validation[ValidationError,MessageType] = {
    messageType.toLowerCase match {
      case "fatal" => MessageType.Fatal.success[ValidationError]
      case "error" => MessageType.Error.success[ValidationError]
      case "warn" => MessageType.Warn.success[ValidationError]
      case "info" => MessageType.Info.success[ValidationError]
      case "debug" => MessageType.Debug.success[ValidationError]
      case _ => ValidationError("invalid message type").fail[MessageType]
    }
  }
}
class LogEntry(val message: String, val messageType:MessageType.MessageType, var stacktrace:Option[String] = None){
  override def toString = Seq(
    s"\n***Client Log Entry***",
    s"${messageType}: ${message}",
    s"${stacktrace.getOrElse("")}",
    s"***End ${new java.util.Date().toString()}***"
  ).mkString("\n")
}
object LogEntry{
  def apply(message:String, strmessageType:String, stackTrace:Option[String] = None):Validation[ValidationError,LogEntry] = {
    MessageType.toType(strmessageType) match {
      case Success(messageType) => new LogEntry(message,messageType,stackTrace).success[ValidationError]
      case Failure(e) => e.failure[LogEntry]
    }
  }
}

class ClientLogger extends Controller with PackageLogging{
  def submitLog(logType:String) = Action(parse.json) { request =>
    val logEntryResult:JsResult[Validation[ValidationError,LogEntry]] = for {
      message <- (request.body \ "message").validate[String]
    } yield LogEntry(message,logType)
    logEntryResult match {
      case JsSuccess(Success(logEntry),_) => {
        logEntry.stacktrace = (request.body \ "stacktrace").asOpt[String]
        logEntry.messageType match {
          case MessageType.Fatal => logger.error(logEntry.toString)
          case MessageType.Error => logger.error(logEntry.toString)
          case MessageType.Warn => logger.warn(logEntry.toString)
          case MessageType.Info => logger.info(logEntry.toString)
          case MessageType.Debug => logger.debug(logEntry.toString)
        }
        Ok
      }
      case JsSuccess(Failure(error),_) => BadRequest(JsObject(Seq(
        "errors" -> JsArray(Seq(JsString(error.message)))
      )))
      case JsError(errors) => BadRequest(JsObject(Seq(
        "errors" -> JsArray(errors.flatMap(_._2).map(e => JsString(e.message)).seq)
      )))
    }
  }
}
object ClientLogger extends ClientLogger()
