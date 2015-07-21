package org.corespring.itemSearch

import org.corespring.models.json.JsonUtil
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
                          sort: Seq[Sort] = ItemIndexQuery.Defaults.sort,
                          metadata: Map[String, String] = ItemIndexQuery.Defaults.metadata,
                          requiredPlayerWidth: Option[Int] = ItemIndexQuery.Defaults.requiredPlayerWidth )

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
    val requiredPlayerWidth = None
    val text = None
    val contributors = Seq.empty[String]
    val collections = Seq.empty[String]
    val itemTypes = Seq.empty[String]
    val gradeLevels = Seq.empty[String]
    val published = None
    val workflows = Seq.empty[String]
    val sort = Seq.empty[Sort]
    val metadata = Map.empty[String, String]
  }

  object Fields {
    val offset = "offset"
    val count = "count"
    val text = "text"
    val contributors = "contributors"
    val collections = "collections"
    val itemTypes = "itemTypes"
    val gradeLevels = "gradeLevels"
    val published = "published"
    val workflows = "workflows"
    val requiredPlayerWidth = "requiredPlayerWidth"
    val sort = "sort"
    val all = Set(offset, count, text, contributors, collections, itemTypes, gradeLevels, published, workflows, sort)
  }

  /**
   * Reads JSON in the format provided by requests to the search API.
   */
  object ApiReads extends Reads[ItemIndexQuery] with JsonUtil {
    import Fields._
    implicit val SortReads = Sort.Reads

    override def reads(json: JsValue): JsResult[ItemIndexQuery] = JsSuccess(
      ItemIndexQuery(
        offset = (json \ offset).asOpt[Int].getOrElse(Defaults.offset),
        count = (json \ count).asOpt[Int].getOrElse(Defaults.count),
        text = (json \ text).asOpt[String],
        contributors = (json \ contributors).asOpt[Seq[String]].getOrElse(Defaults.contributors),
        collections = (json \ collections).asOpt[Seq[String]].getOrElse(Defaults.collections),
        itemTypes = (json \ itemTypes).asOpt[Seq[String]].getOrElse(Defaults.itemTypes),
        gradeLevels = (json \ gradeLevels).asOpt[Seq[String]].getOrElse(Defaults.gradeLevels),
        published = (json \ published).asOpt[Boolean],
        workflows = (json \ workflows).asOpt[Seq[String]].getOrElse(Defaults.workflows),
        requiredPlayerWidth = (json \ requiredPlayerWidth).asOpt[Int],
        sort = (json \ sort).asOpt[JsValue].map(sort => Seq(Json.fromJson[Sort](sort)
          .getOrElse(throw new Exception(s"Could not parse sort object ${(json \ "sort")}"))))
          .getOrElse(Defaults.sort),
        metadata = (json match {
          case jsObject: JsObject =>
            (jsObject.keys diff all).map(key => (jsObject \ key).asOpt[String].map(value => key -> value)).flatten.toMap
          case _ => Map.empty[String, String]
        })
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

    private def range[A <: Int](field: String, gte: Option[A] = None, gt: Option[A] = None, lte: Option[A] = None, lt: Option[A] = None)
                        (implicit writes: Writes[A]): Option[JsObject] =
      if ((gte ++ gt ++ lte ++ lt).isEmpty) None
      else
        Some(Json.obj(
          "range" -> Json.obj(
            field -> partialObj(
              "gte" -> gte.map(JsNumber(_)),
              "gt" -> gt.map(JsNumber(_)),
              "lte" -> lte.map(JsNumber(_)),
              "lt" -> lt.map(JsNumber(_))
            )
          )
        ))


    private def must(metadata: Map[String, String]): Option[JsObject] = {
      metadata.nonEmpty match {
        case true => Some(Json.obj("must" -> metadata.map{ case(key, value) => {
          Json.obj("nested" -> Json.obj(
            "path" -> "metadata",
            "query" -> Json.obj(
              "bool" -> Json.obj(
                "must" -> Json.arr(
                  Json.obj("match" -> Json.obj("metadata.key" -> key)),
                  Json.obj("match" -> Json.obj("metadata.value" -> value))
                )
              )
            )
          ))
        }}))
        case _ => None
      }
    }

    private def should(text: Option[String]): Option[JsObject] = text match {
      case Some("") => None
      case Some(text) => Some(Json.obj("should" -> Json.arr(
        Json.obj("multi_match" -> Json.obj(
          "query" -> text,
          "fields" -> Seq("taskInfo.description", "taskInfo.title", "content"),
          "type" -> "phrase"
        )),
        Json.obj("ids" -> Json.obj(
          "values" -> Json.arr(text)
        ))
      )))
      case _ => None
    }

    def writes(query: ItemIndexQuery): JsValue = {
      import query._
      implicit val SortWrites = Sort.ElasticSearchWrites

      val clauses = Seq(must(metadata), should(text)).flatten.foldLeft(Json.obj()){ case (obj, acc) => acc ++ obj }

      partialObj(
        "from" -> Some(JsNumber(offset)),
        "size" -> Some(JsNumber(count)),
        "query" -> (clauses.keys.nonEmpty match {
          case true => Some(Json.obj("bool" -> clauses))
          case _ => None
        }),
        "filter" -> Some(Json.obj(
          "bool" -> Json.obj("must" -> {
            // need an explicit val, because Scala can't infer this type
            val t: Seq[JsObject] = Seq(
              terms("contributorDetails.contributor", contributors),
              terms("collectionId", collections),
              terms("taskInfo.itemTypes", itemTypes),
              terms("taskInfo.gradeLevel", gradeLevels),
              term("published", published),
              terms("workflow", workflows , Some("and")),
              range("minimumWidth", lte = requiredPlayerWidth)
            ).flatten
            t
          })
        )),
        "sort" -> (query.sort.nonEmpty match {
          case true => Some(JsArray(query.sort.map(Json.toJson(_))))
          case _ => None
        })
      )
    }
  }

}
