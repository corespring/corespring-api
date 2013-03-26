package models.itemSession

import org.bson.types.ObjectId
import models.Standard
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import models.item.Item

case class ItemSessionSummary(itemId: ObjectId,
                              sessionId: ObjectId,
                              itemTitle: String,
                              isComplete: Boolean,
                              itemStandards: Seq[String] = Seq(),
                              score: Option[Double] = Some(0.0)
                               )

object ItemSessionSummary {

  implicit object Writes extends Writes[ItemSessionSummary] {

    def writes(iss: ItemSessionSummary): JsValue = {
      val fields =
        Seq("itemId" -> JsString(iss.itemId.toString),
          "sessionId" -> JsString(iss.sessionId.toString),
          "itemTitle" -> JsString(iss.itemTitle),
          "itemStandards" -> JsArray(iss.itemStandards.map(toJson(_))),
          "isComplete" -> JsBoolean(iss.isComplete)) ++ Seq(iss.score.map("score" -> JsNumber(_))).flatten

      JsObject(fields)
    }
  }

  def apply(session: ItemSession, item: Item): ItemSessionSummary = {

    val finalScore : Option[Double] = if(session.isFinished){
      val (score:Double,total:Double) = ItemSession.getTotalScore(session)
      Some(score/total)
    }
    else None

    new ItemSessionSummary(
      item.id,
      session.id,
      item.taskInfo.get.title.getOrElse(""),
      session.isFinished,
      item.standards,
      finalScore)
  }

}

