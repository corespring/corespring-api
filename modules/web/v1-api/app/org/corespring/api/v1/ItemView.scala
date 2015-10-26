package org.corespring.api.v1

import org.corespring.legacy.ServiceLookup
import org.corespring.models.item.Item
import org.corespring.models.json.VersionedIdFormat
import org.corespring.platform.core.models.search.SearchFields
import play.api.libs.json._

case class ItemView(item: Item, searchFields: Option[SearchFields])

object ItemView {

  import Item.Keys._

  implicit object Writes extends Writes[ContentView[Item]] {

    val jsonFormatting = ServiceLookup.jsonFormatting

    import jsonFormatting._

    def writes(itemView: ContentView[Item]): JsValue = {

      def toJsObject[T](a: Option[T])(implicit w: Writes[T]): Option[JsObject] = {
        a.map(w.writes(_).asInstanceOf[JsObject])
      }

      val mainItem: JsObject = writeMainItem(itemView.content)
      val details: Option[JsObject] = toJsObject(itemView.content.contributorDetails)
      val taskInfo: Option[JsObject] = toJsObject(itemView.content.taskInfo)
      val alignments: Option[JsObject] = toJsObject(itemView.content.otherAlignments)
      val out = Seq(Some(mainItem), details, taskInfo, alignments).flatten
      val jsObject = out.tail.foldRight(out.head)(_ ++ _)
      itemView.searchFields.map { _.processJson(jsObject) }.getOrElse(jsObject)
    }

    private def writeMainItem(item: Item): JsObject = {

      implicit val VersionedIdWrites = VersionedIdFormat

      val basics: Seq[Option[(String, JsValue)]] = Seq(
        Some(("id" -> Json.toJson(item.id))),
        Some("format" -> Json.obj("apiVersion" -> item.createdByApiVersion, "hasQti" -> item.hasQti, "hasPlayerDefinition" -> item.hasPlayerDefinition)),
        item.workflow.map((workflow -> Json.toJson(_))),
        item.data.map((data -> Json.toJson(_))),
        Some(collectionId -> JsString(item.collectionId)),
        item.playerDefinition.map("playerDefinition" -> Json.toJson(_)),
        Some(contentType -> JsString(Item.contentType)),
        Some(published -> JsBoolean(item.published)))

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
        (priorUse, item.priorUse)).map(makeJsString)

      def makeJsArray(tuple: (String, Seq[JsValue])) = {
        val (key, value) = tuple
        if (value.isEmpty)
          None
        else
          Some(key, JsArray(value))
      }

      val standardService = ServiceLookup.standardService
      val validStandards = item.standards.map(standardService.findOneByDotNotation).flatten

      val arrays: Seq[Option[(String, JsValue)]] = Seq(
        (priorGradeLevel, item.priorGradeLevels.map(JsString(_))),
        (reviewsPassed, item.reviewsPassed.map(JsString(_))),
        (supportingMaterials, item.supportingMaterials.map(Json.toJson(_))),
        (standards, validStandards.map(Json.toJson(_)))).map(makeJsArray)

      val joined = (basics ++ strings ++ arrays).flatten
      JsObject(joined)
    }
  }
}
