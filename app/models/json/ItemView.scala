package models.json

import models.{Standard, ContentType, Item}
import play.api.libs.json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import models.search.SearchFields

case class ItemView(item:Item, searchFields:Option[SearchFields])
object ItemView{
  implicit object ItemViewWrites extends Writes[ItemView]{
    def writes(itemView: ItemView): JsValue = {
      //if(itemView.searchFields.isDefined) itemView.searchFields.get.addDbFieldsToJsFields

      def toJsObject[T](a: Option[T])(implicit w: Writes[T]): Option[JsObject] = a.map(w.writes(_).asInstanceOf[JsObject])

      val mainItem: JsObject = writeMainItem(itemView.item)
      val details: Option[JsObject] = toJsObject(itemView.item.contributorDetails)
      val taskInfo: Option[JsObject] = toJsObject(itemView.item.taskInfo)
      val alignments: Option[JsObject] = toJsObject(itemView.item.otherAlignments)

      val out = Seq(Some(mainItem), details, taskInfo, alignments).flatten
      val jsObject = out.tail.foldRight(out.head)(_ ++ _)
      itemView.searchFields.map(stripFields(jsObject,_)).getOrElse(jsObject)
    }
    private def stripFields(jsObject:JsObject, searchFields:SearchFields):JsObject = {
      def checkFields(key:String):Boolean = if(searchFields.inclusion) searchFields.jsfields.exists(_ == key)
          else searchFields.jsfields.exists(_ != key)

      JsObject(jsObject.fields.foldRight[Seq[(String,JsValue)]](Seq())((field,result) => {
          if(checkFields(field._1)) result :+ field
          else result
        })
      )
    }
    private def writeMainItem(item: Item): JsObject = {

      val basics: Seq[Option[(String, JsValue)]] = Seq(
        Some(("id" -> JsString(item.id.toString))),
        item.workflow.map((Item.workflow -> Json.toJson(_))),
        item.data.map((Item.data -> Json.toJson(_))),
        Some((Item.collectionId -> JsString(item.collectionId))),
        Some(Item.contentType -> JsString(ContentType.item))
      )

      def makeJsString(tuple: (String, Option[String])) = {
        val (key, value) = tuple
        value match {
          case Some(s) => Some((key, JsString(s)))
          case _ => None
        }
      }

      val strings: Seq[Option[(String, JsValue)]] = Seq(
        (Item.lexile, item.lexile),
        (Item.originId, item.originId),
        (Item.pValue, item.pValue),
        (Item.priorUse, item.priorUse)
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
        (Item.priorGradeLevel, item.priorGradeLevel.map(JsString(_))),
        (Item.reviewsPassed, item.reviewsPassed.map(JsString(_))),
        (Item.supportingMaterials, item.supportingMaterials.map(Json.toJson(_))),
        (Item.standards, validStandards.map(Json.toJson(_)))
      ).map(makeJsArray)

      val joined = (basics ++ strings ++ arrays).flatten
      JsObject(joined)
    }
  }
}
