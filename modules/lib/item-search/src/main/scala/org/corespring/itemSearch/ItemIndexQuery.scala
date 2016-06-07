package org.corespring.itemSearch

import org.bson.types.ObjectId
import org.corespring.models.json.JsonUtil
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger
import play.api.libs.json.Json._
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
  widgets: Seq[String] = ItemIndexQuery.Defaults.widgets,
  gradeLevels: Seq[String] = ItemIndexQuery.Defaults.gradeLevels,
  published: Option[Boolean] = ItemIndexQuery.Defaults.published,
  standardClusters: Seq[String] = ItemIndexQuery.Defaults.standardClusters,
  standards: Seq[String] = ItemIndexQuery.Defaults.standards,
  workflows: Seq[String] = ItemIndexQuery.Defaults.workflows,
  sort: Seq[Sort] = ItemIndexQuery.Defaults.sort,
  metadata: Map[String, String] = ItemIndexQuery.Defaults.metadata,
  requiredPlayerWidth: Option[Int] = ItemIndexQuery.Defaults.requiredPlayerWidth) {

  def scopeToCollections(collectionIds: String*): ItemIndexQuery = {
    val scopedCollections = collections.filter(c => collectionIds.exists(_ == c)) match {
      case Nil => collectionIds
      case c: Seq[String] => c
    }
    this.copy(collections = scopedCollections)
  }

  def versionedId: Option[VersionedId[ObjectId]] = for {
    t <- text
    vid <- VersionedId(t.trim)
  } yield vid
}

case class Sort(field: String, direction: Option[String])

object Sort {

  private val fieldMapping = Map(
    "title" -> "taskInfo.title.raw",
    "description" -> "taskInfo.description.raw",
    "subject" -> "taskInfo.subjects.primary.subject",
    "gradeLevel" -> "taskInfo.gradeLevel",
    "itemType" -> "taskInfo.itemTypes",
    "widget" -> "taskInfo.widgets",
    "standard" -> "standards.dotNotation",
    "standardClusters" -> "taskInfo.standardClusters",
    "contributor" -> "contributorDetails.contributor")

  object ElasticSearchWrites extends Writes[Sort] {
    override def writes(sort: Sort): JsValue = obj(
      fieldMapping.get(sort.field).getOrElse(sort.field) -> obj(
        "order" -> (sort.direction match {
          case Some("desc") => "desc"
          case _ => "asc"
        })))
  }

  object Reads extends Reads[Sort] {
    override def reads(json: JsValue): JsResult[Sort] = json match {
      case obj: JsObject => JsSuccess(Sort(
        field = obj.keys.head,
        direction = (obj \ (obj.keys.head)).asOpt[String]))
      case _ => JsError("Must be object")
    }
  }

}

object ItemIndexQuery {

  /**
   * Default query values
   */
  object Defaults {
    val count = 50
    val offset = 0
    val collections = Seq.empty[String]
    val contributors = Seq.empty[String]
    val gradeLevels = Seq.empty[String]
    val itemTypes = Seq.empty[String]
    val metadata = Map.empty[String, String]
    val published = None
    val requiredPlayerWidth = None
    val sort = Seq.empty[Sort]
    val standardClusters = Seq.empty[String]
    val standards = Seq.empty[String]
    val text = None
    val widgets = Seq.empty[String]
    val workflows = Seq.empty[String]
  }

  object Fields {
    val collections = "collections"
    val contributors = "contributors"
    val count = "count"
    val gradeLevels = "gradeLevels"
    val itemTypes = "itemTypes"
    val offset = "offset"
    val published = "published"
    val requiredPlayerWidth = "requiredPlayerWidth"
    val sort = "sort"
    val standardClusters = "standardClusters"
    val standards = "standards"
    val text = "text"
    val widgets = "widgets"
    val workflows = "workflows"

    val all = Set(
      collections,
      contributors,
      count,
      gradeLevels,
      itemTypes,
      offset,
      published,
      requiredPlayerWidth,
      sort,
      standardClusters,
      standards,
      text,
      widgets,
      workflows)
  }

  /**
   * Reads JSON in the format provided by requests to the search API.
   */
  object ApiReads extends Reads[ItemIndexQuery] with JsonUtil {
    import Fields._
    implicit val SortReads = Sort.Reads

    override def reads(json: JsValue): JsResult[ItemIndexQuery] = JsSuccess(
      ItemIndexQuery(
        collections = (json \ collections).asOpt[Seq[String]].getOrElse(Defaults.collections),
        contributors = (json \ contributors).asOpt[Seq[String]].getOrElse(Defaults.contributors),
        count = (json \ count).asOpt[Int].getOrElse(Defaults.count),
        gradeLevels = (json \ gradeLevels).asOpt[Seq[String]].getOrElse(Defaults.gradeLevels),
        itemTypes = (json \ itemTypes).asOpt[Seq[String]].getOrElse(Defaults.itemTypes),
        metadata = (json match {
          case jsObject: JsObject =>
            (jsObject.keys diff all).map(key => (jsObject \ key).asOpt[String].map(value => key -> value)).flatten.toMap
          case _ => Map.empty[String, String]
        }),
        offset = (json \ offset).asOpt[Int].getOrElse(Defaults.offset),
        published = (json \ published).asOpt[Boolean],
        requiredPlayerWidth = (json \ requiredPlayerWidth).asOpt[Int],
        sort = (json \ sort).asOpt[JsValue].map(sort => Seq(Json.fromJson[Sort](sort)
          .getOrElse(throw new Exception(s"Could not parse sort object ${(json \ "sort")}"))))
          .getOrElse(Defaults.sort),
        standardClusters = (json \ standardClusters).asOpt[Seq[String]].getOrElse(Defaults.standardClusters),
        standards = (json \ standards).asOpt[Seq[String]].getOrElse(Defaults.standards),
        text = (json \ text).asOpt[String],
        widgets = (json \ widgets).asOpt[Seq[String]].getOrElse(Defaults.widgets),
        workflows = (json \ workflows).asOpt[Seq[String]].getOrElse(Defaults.workflows)))
  }

  /**
   * Writes the query to a JSON format understood by Elastic Search.
   */
  object ElasticSearchWrites extends Writes[ItemIndexQuery] with JsonUtil {

    implicit class JsValueImplicits(js: JsValue) {
      implicit def isEmpty: Boolean = js match {
        case o: JsObject => o.fields.length == 0
        case s: JsString => s.value.isEmpty
        case a: JsArray => a.value.length == 0
        case u: JsUndefined => true
        case _ => false
      }
    }

    private lazy val logger = Logger(ElasticSearchWrites.getClass)

    private def terms[A](field: String, values: Seq[A], execution: Option[String] = None)(implicit writes: Writes[A]) = filter("terms", field, values, execution): Option[JsObject]

    private def term[A](field: String, values: Option[A])(implicit writes: Writes[A], execution: Option[String] = None): Option[JsObject] =
      filter("term", field, values, execution)

    private def filter[A](named: String, field: String, values: Seq[A], execution: Option[String])(implicit writes: Writes[A]): Option[JsObject] =
      values.nonEmpty match {
        case true => Some(obj(named -> partialObj(
          field -> Some(Json.toJson(values)), "execution" -> execution.map(JsString))))
        case _ => None
      }

    private def filter[A](named: String, field: String, value: Option[A], execution: Option[String])(implicit writes: Writes[A]): Option[JsObject] =
      value.map(v => partialObj(
        named -> Some(obj(field -> Json.toJson(v))), "execution" -> execution.map(JsString)))

    private def range[A <: Int](field: String, gte: Option[A] = None, gt: Option[A] = None, lte: Option[A] = None, lt: Option[A] = None)(implicit writes: Writes[A]): Option[JsObject] =
      if ((gte ++ gt ++ lte ++ lt).isEmpty) None
      else
        Some(obj(
          "range" -> obj(
            field -> partialObj(
              "gte" -> gte.map(JsNumber(_)),
              "gt" -> gt.map(JsNumber(_)),
              "lte" -> lte.map(JsNumber(_)),
              "lt" -> lt.map(JsNumber(_))))))

    private def must(query: ItemIndexQuery, extras: JsObject*): JsObject = {

      val base = query.published
        .flatMap { p => term("published", Some(p)) }
        .orElse {
          //If a versionedId is specified - then latest:true doesn't apply
          query.versionedId.filter(_.version.isDefined)
            .map { _ => obj() }
            .orElse(term("latest", Some(true)))
        }
        .getOrElse(obj())

      logger.trace(s"function=should, base=$base")

      val metadataQuery: Seq[JsValue] = query.metadata.toSeq.map {
        case (key, value) => {
          obj("nested" -> obj(
            "path" -> "metadata",
            "query" -> obj(
              "bool" -> obj(
                "must" -> arr(
                  obj("match" -> obj("metadata.key" -> key)),
                  obj("match" -> obj("metadata.value" -> value)))))))
        }
      }

      val allClauses: Seq[JsValue] = (base +: metadataQuery) ++ extras
      obj("must" -> allClauses.filter(!_.isEmpty))
    }

    private def should(query: ItemIndexQuery): Option[JsObject] = {

      query.text.map { t =>
        val fields: JsArray = arr(
          "taskInfo.description",
          "taskInfo.title",
          "content",
          "taskInfo.standardClusters")

        val clauses = arr(

          obj(
            "term" -> obj(
              "id" -> t)),
          obj(
            "multi_match" -> obj(
              "query" -> t,
              "fields" -> fields,
              "type" -> "phrase")),
          obj(
            "ids" -> obj(
              "values" -> arr(t))))
        val all: JsArray = clauses
        obj("should" -> all)
      }
    }

    def writes(query: ItemIndexQuery): JsValue = {
      import query._
      implicit val SortWrites = Sort.ElasticSearchWrites

      val shouldQuery = should(query).map(s => obj("bool" -> s)).toSeq
      val mustQuery = must(query, shouldQuery: _*)

      partialObj(
        "from" -> Some(JsNumber(offset)),
        "size" -> Some(JsNumber(count)),
        "query" -> Some(obj("bool" -> mustQuery)),
        "aggs" -> Some(obj(
          "id_count" -> obj(
            "cardinality" -> obj("field" -> "id")))),
        "filter" -> Some(obj(
          "bool" -> obj("must" -> {
            // need an explicit val, because Scala can't infer this type
            val t: Seq[JsObject] = Seq(
              terms("contributorDetails.contributor", contributors),
              terms("collectionId", collections),
              terms("taskInfo.itemTypes", itemTypes),
              terms("taskInfo.widgets", widgets),
              terms("taskInfo.gradeLevel", gradeLevels),
              terms("taskInfo.standardClusters", standardClusters),
              terms("standards.dotNotation", standards),
              terms("workflow", workflows, Some("and")),
              range("minimumWidth", lte = requiredPlayerWidth)).flatten
            t
          }))),
        "sort" -> (query.sort.nonEmpty match {
          case true => Some(JsArray(query.sort.map(Json.toJson(_))))
          case _ => None
        }))
    }
  }

}
