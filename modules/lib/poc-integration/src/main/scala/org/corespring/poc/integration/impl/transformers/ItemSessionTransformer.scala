package org.corespring.poc.integration.impl.transformers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.{ItemSessionSettings, ItemSession}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qti.models.responses.{StringResponse, ArrayResponse, Response}
import play.api.Logger
import play.api.libs.json._
import org.joda.time.DateTime

trait ItemSessionTransformer {
  def toPocJson(session: ItemSession): JsValue

  def toItemSession(pocJson: JsValue): ItemSession
}

object ItemSessionTransformer extends ItemSessionTransformer {

  private val logger = Logger("poc.integration")

  def toPocJson(session: ItemSession): JsValue = {

    def settingsJson: JsObject = Json.obj(
      "maxNoOfAttempts" -> JsNumber(session.settings.maxNoOfAttempts),
      "showCorrectResponse" -> JsBoolean(session.settings.highlightCorrectResponse),
      "showFeedback" -> JsBoolean(session.settings.showFeedback),
      "showUserResponse" -> JsBoolean(session.settings.highlightUserResponse))

    def answers: JsObject = {

      def response(r: Response): JsValue = {
        r match {
          case StringResponse(id, value, _) => Json.obj("value" -> JsString(value))
          case ArrayResponse(id, value, _) => Json.obj("value" -> JsArray(value.map(JsString(_))))
        }
      }

      val out: Seq[(String, JsValue)] = session.responses.map {
        r => (r.id, response(r))
      }
      JsObject(out)
    }

    def sessionJson = {

      val out : Seq[(String,JsValue)] = Seq(
        "id" -> JsString(session.id.toString),
        "itemId" -> JsString(session.itemId.toString),
        "answers" -> answers,
        "isFinished" -> JsBoolean(session.isFinished),
        "remainingAttempts" -> JsNumber(session.settings.maxNoOfAttempts - session.attempts)
      )

      val maybes: Seq[(String, JsValue)] = Seq(
        session.start.map(s => ("start" -> JsNumber(s.getMillis))
        )).flatten

      JsObject(out ++ maybes) ++ settingsJson
    }

    sessionJson
  }

  /**
   * {
   * "itemId":"5252c7c108738c6d85653725",
   * "maxNoOfAttempts":1,
   * "showCorrectResponse":true,
   * "showFeedback":true,
   * "showUserResponse":true,
   * "remainingAttempts":0,
   * "answers":
   * {"Q_01":
   * {"value":"ChoiceA text (Correct Choice)"}
   * },
   * "isFinished":true
   * }
   * @param pocJson
   * @return
   */
  def toItemSession(pocJson: JsValue): ItemSession = {
    logger.debug(Json.stringify(pocJson))
    val itemId = VersionedId((pocJson \ "itemId").as[String]).getOrElse(throw new RuntimeException("Invalid versioned id"))

    def makeResponses(answers: JsObject): Seq[Response] = answers.fields.map {
      (f: (String, JsValue)) =>
        val (key, json) = f
        (json \ "value") match {
          case jsonArray: JsArray => ArrayResponse(id = key, responseValue = jsonArray.as[Seq[String]])
          case jsonString: JsString => StringResponse(id = key, responseValue = jsonString.as[String])
          case _ => StringResponse(id = key, responseValue = "??")
        }
    }

    def makeSettings(settings: JsObject): ItemSessionSettings = {
      ItemSessionSettings(
        maxNoOfAttempts = (settings \ "maxNoOfAttempts").as[Int],
        highlightCorrectResponse = (settings \ "showCorrectResponse").as[Boolean],
        highlightUserResponse = (settings \ "showUserResponse").as[Boolean],
        showFeedback = (settings \ "showFeedback").as[Boolean]
      )
    }
    val responses = makeResponses((pocJson \ "answers").as[JsObject])

    val settings: ItemSessionSettings = makeSettings(pocJson.as[JsObject])

    val attemptsCount = (pocJson \ "remainingAttempts").asOpt[Int].map {
      remaining =>
        settings.maxNoOfAttempts - remaining
    }.getOrElse(0)

    val out = ItemSession(
      id = new ObjectId((pocJson \ "id").as[String]),
      itemId = itemId,
      responses = responses,
      attempts = attemptsCount,
      settings = settings,
      start = (pocJson \ "start").asOpt[Long].map(new DateTime(_)),
      finish = (pocJson \ "finish").asOpt[Long].map(new DateTime(_))
    )

    out
  }
}
