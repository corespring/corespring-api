package org.corespring.platform.core.services.item

import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._

/**
 * Contains fields used for querying the item index
 */
case class ItemIndexQuery(offset: Option[Int] = ItemIndexQuery.Defaults.offset,
                          count: Option[Int] = ItemIndexQuery.Defaults.count,
                          text: Option[String] = ItemIndexQuery.Defaults.text,
                          contributors: Option[Seq[String]] = ItemIndexQuery.Defaults.contributors,
                          collections: Option[Seq[String]] = ItemIndexQuery.Defaults.collections,
                          itemTypes: Option[Seq[String]] = ItemIndexQuery.Defaults.itemTypes,
                          gradeLevels: Option[Seq[String]] = ItemIndexQuery.Defaults.gradeLevels,
                          published: Option[Boolean] = ItemIndexQuery.Defaults.published,
                          workflows: Option[Seq[String]] = ItemIndexQuery.Defaults.workflows)

object ItemIndexQuery {

  /**
   * Default query values
   */
  object Defaults {
    val offset = Some(0)
    val count = Some(50)
    val text = None
    val contributors = None
    val collections = None
    val itemTypes = None
    val gradeLevels = None
    val published = None
    val workflows = None
  }

  /**
   * Writes the query to a JSON format understood by Elastic Search.
   */
  object ElasticSearchWrites extends Writes[ItemIndexQuery] with JsonUtil {

    private def terms[A](field: String, values: Option[Seq[A]], execution: Option[String] = None)
                        (implicit writes: Writes[A]) = filter("terms", field, values, execution): Option[JsObject]
    private def term[A](field: String, values: Option[A])
                       (implicit writes: Writes[A], execution: Option[String] = None): Option[JsObject] =
      filter("term", field, values, execution)

    private def filter[A](named: String, field: String, values: Option[A], execution: Option[String])
                         (implicit writes: Writes[A]): Option[JsObject] =
      values match {
        case Some(values: Seq[A]) => values.nonEmpty match {
          case true => Some(Json.obj(named -> partialObj(
            field -> Some(Json.toJson(values)), "execution" -> execution.map(JsString)
          )))
          case _ => None
        }
        case Some(value) => Some(partialObj(
          named -> Some(Json.obj(field -> Json.toJson(value))), "execution" -> execution.map(JsString)))
        case _ => None
      }

    def writes(query: ItemIndexQuery): JsValue = {
      import query._
      Json.obj(
        "from" -> offset,
        "size" -> count,
        "query" -> Json.obj(
          "filtered" -> partialObj(
            "query" -> query.text.map(text => Json.obj(
              "simple_query_string" -> Json.obj(
                "query" -> text
              )
            )),
            "filter" -> Some(Json.obj(
              "bool" -> Json.obj("must" -> {
                // need an explicit val, because Scala can't infer this type
                val t: Seq[JsObject] = Seq(
                  terms("contributorDetails.contributor", contributors),
                  terms("collectionId", collections),
                  terms("taskInfo.itemTypes", itemTypes),
                  terms("taskInfo.gradeLevel", gradeLevels),
                  term("published", published),
                  terms("workflow", workflows , Some("and"))
                ).flatten
                t
              })
            ))
          )
        )
      )
    }

  }

}
