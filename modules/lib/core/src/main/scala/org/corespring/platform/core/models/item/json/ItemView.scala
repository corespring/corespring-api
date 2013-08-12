package org.corespring.platform.core.models.json

import play.api.libs.json._
import scala.Some
import org.corespring.platform.core.models.Standard
import org.corespring.platform.core.models.search.SearchFields
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.models.item.{Item, ContentType}
import org.corespring.platform.core.services.item.ItemServiceImpl

case class ItemView(item:Item, searchFields:Option[SearchFields])

object ItemView{

  import Item.Keys._

  implicit object ItemViewWrites extends Writes[ItemView]{
    def writes(itemView: ItemView): JsValue = {

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

      import VersionedIdImplicits.Writes

      val basics: Seq[Option[(String, JsValue)]] = Seq(
        Some(("id" -> Json.toJson(item.id))),
        item.workflow.map((workflow -> Json.toJson(_))),
        item.data.map((data -> Json.toJson(_))),
        Some((collectionId -> JsString(item.collectionId))),
        Some(contentType -> JsString(ContentType.item)),
        Some(published -> JsBoolean(item.published)),
        Some("sessionCount" -> JsNumber(ItemServiceImpl.sessionCount(item)))
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
