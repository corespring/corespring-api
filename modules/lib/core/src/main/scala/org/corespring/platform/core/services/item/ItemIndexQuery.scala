package org.corespring.platform.core.services.item

import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._

/**
 * Contains fields used for querying the item index
 */
case class ItemIndexQuery(offset: Int = ItemIndexQuery.Defaults.offset,
                          count: Int = ItemIndexQuery.Defaults.count,
                          text: Option[String] = ItemIndexQuery.Defaults.text,
                          contributors: Seq[String] = ItemIndexQuery.Defaults.contributors,
                          collections: Seq[String] = ItemIndexQuery.Defaults.collections,
                          itemTypes: Seq[String] = ItemIndexQuery.Defaults.itemTypes,
                          gradeLevels: Seq[String] = ItemIndexQuery.Defaults.gradeLevels,
                          published: Option[Boolean] = ItemIndexQuery.Defaults.published,
                          workflows: Seq[String] = ItemIndexQuery.Defaults.workflows,
                          sort: Seq[Sort] = ItemIndexQuery.Defaults.sort)

case class Sort(field: String, direction: Option[String])

object Sort {

  private val fieldMapping = Map(
    "title" -> "taskInfo.title.raw",
    "description" -> "taskInfo.description.raw",
    "subject" -> "taskInfo.subjects.primary.subject",
    "gradeLevel" -> "taskInfo.gradeLevel",
    "itemType" -> "taskInfo.itemTypes",
    "standard" -> "taskInfo.standards.dotNotation",
    "contributor" -> "contributorDetails.contributor"
  )

  object ElasticSearchWrites extends Writes[Sort] {
    override def writes(sort: Sort): JsValue = Json.obj(
      fieldMapping.get(sort.field).getOrElse(sort.field) -> Json.obj(
        "order" -> (sort.direction match {
          case Some("desc") => "desc"
          case _ => "asc"
        })
      )
    )
  }

  object Reads extends Reads[Sort] {
    override def reads(json: JsValue): JsResult[Sort] = json match {
      case obj: JsObject => JsSuccess(Sort(
        field = obj.keys.head,
        direction = (obj \ (obj.keys.head)).asOpt[String]
      ))
      case _ => JsError("Must be object")
    }
  }

}

object ItemIndexQuery {

  /**
   * Default query values
   */
  object Defaults {
    val offset = 0
    val count = 50
    val text = None
    val contributors = Seq.empty[String]
    val collections = Seq.empty[String]
    val itemTypes = Seq.empty[String]
    val gradeLevels = Seq.empty[String]
    val published = None
    val workflows = Seq.empty[String]
    val sort = Seq.empty[Sort]
  }

  /**
   * Reads JSON in the format provided by requests to the search API.
   */
  object ApiReads extends Reads[ItemIndexQuery] {
    implicit val SortReads = Sort.Reads

    override def reads(json: JsValue): JsResult[ItemIndexQuery] = JsSuccess(
      ItemIndexQuery(
        offset = (json \ "offset").asOpt[Int].getOrElse(Defaults.offset),
        count = (json \ "count").asOpt[Int].getOrElse(Defaults.count),
        text = (json \ "text").asOpt[String],
        contributors = (json \ "contributors").asOpt[Seq[String]].getOrElse(Defaults.contributors),
        collections = (json \ "collections").asOpt[Seq[String]].getOrElse(Defaults.collections),
        itemTypes = (json \ "itemTypes").asOpt[Seq[String]].getOrElse(Defaults.itemTypes),
        gradeLevels = (json \ "gradeLevels").asOpt[Seq[String]].getOrElse(Defaults.gradeLevels),
        published = (json \ "published").asOpt[Boolean],
        workflows = (json \ "workflows").asOpt[Seq[String]].getOrElse(Defaults.workflows),
        sort = (json \ "sort").asOpt[JsValue].map(sort => Seq(Json.fromJson[Sort](sort)
          .getOrElse(throw new Exception(s"Could not parse sort object ${(json \ "sort")}"))))
          .getOrElse(Defaults.sort)
      )
    )
  }

  /**
   * Writes the query to a JSON format understood by Elastic Search.
   */
  object ElasticSearchWrites extends Writes[ItemIndexQuery] with JsonUtil {

    private def terms[A](field: String, values: Seq[A], execution: Option[String] = None)
                        (implicit writes: Writes[A]) = filter("terms", field, values, execution): Option[JsObject]
    private def term[A](field: String, values: Option[A])
                       (implicit writes: Writes[A], execution: Option[String] = None): Option[JsObject] =
      filter("term", field, values, execution)

    private def filter[A](named: String, field: String, values: Seq[A], execution: Option[String])
                         (implicit writes: Writes[A]): Option[JsObject] =
      values.nonEmpty match {
        case true => Some(Json.obj(named -> partialObj(
          field -> Some(Json.toJson(values)), "execution" -> execution.map(JsString)
        )))
        case _ => None
      }

    private def filter[A](named: String, field: String, value: Option[A], execution: Option[String])
                         (implicit writes: Writes[A]): Option[JsObject] =
      value.map(v => partialObj(
        named -> Some(Json.obj(field -> Json.toJson(v))), "execution" -> execution.map(JsString)))


    def writes(query: ItemIndexQuery): JsValue = {
      import query._
      implicit val SortWrites = Sort.ElasticSearchWrites

      partialObj(
        "from" -> Some(JsNumber(offset)),
        "size" -> Some(JsNumber(count)),
        "query" -> Some(Json.obj(
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
        )),
        "sort" -> (query.sort.nonEmpty match {
          case true => Some(JsArray(query.sort.map(Json.toJson(_))))
          case _ => None
        })
      )
    }

  }

}
