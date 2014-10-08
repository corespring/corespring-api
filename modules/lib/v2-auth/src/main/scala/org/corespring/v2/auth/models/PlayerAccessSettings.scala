package org.corespring.v2.auth.models

import org.corespring.v2.auth.models.Mode.Mode
import play.api.libs.json._

case class PlayerAccessSettings(itemId: String,
  sessionId: Option[String] = None,
  secure: Boolean = false,
  expires: Option[Long] = None,
  mode: Option[String] = None) {

  def allowSessionId(sessionId: String): Boolean = this.sessionId.map {
    s =>
      s == PlayerAccessSettings.STAR || s == sessionId
  }.getOrElse(sessionId == PlayerAccessSettings.STAR)

  def allowItemId(itemId: String): Boolean = this.itemId == PlayerAccessSettings.STAR || this.itemId == itemId

  def allowMode(mode: Mode) = this.mode.map {
    m =>
      m == PlayerAccessSettings.STAR || Mode.withName(m) == mode
  }.getOrElse(true)
}

object PlayerAccessSettings {
  val STAR = "*"
  val ANYTHING = PlayerAccessSettings(STAR, Some(STAR), false)
  val NOTHING = PlayerAccessSettings("____", Some("____"), false)

  def fromJson(s: String) = try {
    optionsFormat.reads(Json.parse(s)) match {
      case JsSuccess(o, _) => Some(o)
      case JsError(errs) => {
        None
      }
    }
  } catch {
    case e: Throwable => {
      None
    }
  }

  implicit val optionsFormat = new Format[PlayerAccessSettings] {
    override def writes(o: PlayerAccessSettings): JsValue = {

      JsObject(
        Seq(
          Some("itemId" -> JsString(o.itemId)),
          o.sessionId.map {
            "sessionId" -> JsString(_)
          },
          Some("secure" -> JsBoolean(o.secure)),
          o.expires.map {
            "expires" -> JsNumber(_)
          },
          o.mode.map {
            "mode" -> JsString(_)
          }).flatten)
    }

    override def reads(json: JsValue): JsResult[PlayerAccessSettings] = {

      val expires: Option[Long] = (json \ "expires") match {
        case JsNumber(n) => Some(n.toLong)
        case JsString(s) => try {
          Some(s.toLong)
        } catch {
          case _ => None
        }
        case _ => None
      }

      JsSuccess {
        PlayerAccessSettings(
          (json \ "itemId").asOpt[String].getOrElse("____"),
          (json \ "sessionId").asOpt[String],
          (json \ "secure").asOpt[Boolean].getOrElse(true),
          expires,
          (json \ "mode").asOpt[String])
      }
    }
  }
}

