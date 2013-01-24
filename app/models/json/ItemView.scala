package models.json

import models.{Standard, Subject, ContentType, Item}
import models.search.SearchFields
import play.api.libs.json._
import scala.Some
import models.search.SearchFields
import scala.Some
import models.search.SearchFields
import scala.Some
import models.search.SearchFields
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import scala.Some
import models.search.SearchFields
import play.api.Logger
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsNumber
import models.search.SearchFields
import models.Workflow.WorkflowWrites

case class ItemView(item:Item, searchFields:Option[SearchFields])
object ItemView{
  implicit object ItemViewWrites extends Writes[ItemView]{
    def writes(itemView: ItemView): JsValue = {
      if(itemView.searchFields.isDefined) itemView.searchFields.get.addDbFieldsToJsFields

      def checkFields(key:String):Boolean = {
        if (itemView.searchFields.isDefined){
          if(itemView.searchFields.get.inclusion){
            itemView.searchFields.get.jsfields.exists(_ == key)
          }else{
            itemView.searchFields.get.jsfields.exists(_ != key)
          }
        } else true
      }
      def toJsObject[T](a: Option[T])(implicit w: Writes[T]): Option[JsObject] = a.map(w.writes(_).asInstanceOf[JsObject])

      val mainItem: JsObject = writeMainItem(itemView.item)
      val details: Option[JsObject] = toJsObject(itemView.item.contributorDetails)
      val taskInfo: Option[JsObject] = toJsObject(itemView.item.taskInfo)
      val alignments: Option[JsObject] = toJsObject(itemView.item.otherAlignments)

      val out = Seq(Some(mainItem), details, taskInfo, alignments).flatten
      val jsObject = out.tail.foldRight(out.head)(_ ++ _)
      jsObject
    }

    private def writeMainItem(item: Item): JsObject = {

      val basics: Seq[Option[(String, JsValue)]] = Seq(
        Some(("id" -> JsString(item.id.toString))),
        item.workflow.map((Item.workflow -> Json.toJson(_))),
        item.data.map((data -> Json.toJson(_))),
        Some((collectionId -> JsString(item.collectionId))),
        Some(contentType -> JsString(ContentType.item))
      )

      def makeJsString(tuple: (String, Option[String])) = {
        val (key, value) = tuple
        value match {
          case Some(s) => Some((key, JsString(s)))
          case _ => None
        }
      }

      val strings: Seq[Option[(String, JsValue)]] = Seq(
        (lexile, item.lexile),
        (originId, item.originId),
        (pValue, item.pValue),
        (priorUse, item.priorUse)
      ).map(makeJsString)

      def makeJsArray(tuple: (String, Seq[JsValue])) = {
        val (key, value) = tuple
        if (value.isEmpty)
          None
        else
          Some(key, JsArray(value))
      }

      val validStandards = item.standards.map(Standard.findOneByDotNotation).flatten

      val arrays: Seq[Option[(String, JsValue)]] = Seq(
        (priorGradeLevel, item.priorGradeLevel.map(JsString(_))),
        (reviewsPassed, item.reviewsPassed.map(JsString(_))),
        (supportingMaterials, item.supportingMaterials.map(Json.toJson(_))),
        (standards, validStandards.map(Json.toJson(_)))
      ).map(makeJsArray)

      val joined = (basics ++ strings ++ arrays).flatten
      JsObject(joined)
    }
  }
}
