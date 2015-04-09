package org.corespring.platform.core.services.item

import play.api.libs.json._

case class ItemIndexQuery(offset: Int = ItemIndexQuery.Defaults.offset,
                          count: Int = ItemIndexQuery.Defaults.count,
                          text: Option[String] = ItemIndexQuery.Defaults.text,
                          contributors: Seq[String] = ItemIndexQuery.Defaults.contributors,
                          collections: Seq[String] = ItemIndexQuery.Defaults.collections,
                          itemTypes: Seq[String] = ItemIndexQuery.Defaults.itemTypes,
                          gradeLevels: Seq[String] = ItemIndexQuery.Defaults.gradeLevels,
                          published: Option[Boolean] = ItemIndexQuery.Defaults.published,
                          workflows: Seq[String] = ItemIndexQuery.Defaults.workflows)

object ItemIndexQuery {

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
  }

  object ApiReads extends Reads[ItemIndexQuery] {
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
        workflows = (json \ "workflows").asOpt[Seq[String]].getOrElse(Defaults.workflows)
      )
    )
  }

  object ElasticSearchWrites extends Writes[ItemIndexQuery] {

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


    override def writes(query: ItemIndexQuery): JsValue = {
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

    def partialObj(fields : (String, Option[JsValue])*): JsObject =
      JsObject(fields.filter{ case (_, v) => v.nonEmpty }.map{ case (a,b) => (a, b.get) })
  }

}
