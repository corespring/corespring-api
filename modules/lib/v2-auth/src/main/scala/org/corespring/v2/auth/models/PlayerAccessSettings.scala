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
  val ANYTHING = PlayerAccessSettings(STAR, Some(STAR), false, expires = Some(0))
  val NOTHING = PlayerAccessSettings("____", Some("____"), false)

  def readExpires(expires: JsValue): Option[Long] =
    expires match {
      case JsNumber(n) => Some(n.toLong)
      case JsString(s) => try {
        Some(s.toLong)
      } catch {
        case e:Throwable => None
      }
      case _ => None
    }

  def permissiveRead(json: JsValue): JsResult[PlayerAccessSettings] = readExpires(json \ "expires").map { e =>
    JsSuccess(PlayerAccessSettings(
      itemId = (json \ "itemId").asOpt[String].getOrElse(STAR),
      sessionId = (json \ "sessionId").asOpt[String].orElse(Some(STAR)),
      mode = (json \ "mode").asOpt[String].orElse(Some(STAR)),
      expires = Some(e),
      secure = (json \ "secure").asOpt[Boolean].getOrElse(false)))
  }.getOrElse(JsError("Missing 'expires'"))

  implicit val format = new Format[PlayerAccessSettings] {
    override def writes(o: PlayerAccessSettings): JsValue = {

      JsObject(
        Seq(
          Some("itemId" -> JsString(o.itemId)),
          o.sessionId.map {
            "sessionId" -> JsString(_)
          },
          Some("secure" -> JsBoolean(o.secure)),
          o.expires.map { e =>
            "expires" -> JsString(e.toString)
          },
          o.mode.map {
            "mode" -> JsString(_)
          }).flatten)
    }

    override def reads(json: JsValue): JsResult[PlayerAccessSettings] = {

      readExpires(json \ "expires") match {
        case Some(e) => {
          JsSuccess {
            PlayerAccessSettings(
              (json \ "itemId").asOpt[String].getOrElse("____"),
              (json \ "sessionId").asOpt[String],
              (json \ "secure").asOpt[Boolean].getOrElse(true),
              Some(e),
              (json \ "mode").asOpt[String])
          }
        }
        case _ => JsError("Missing 'expires'")
      }
    }

  }
}

