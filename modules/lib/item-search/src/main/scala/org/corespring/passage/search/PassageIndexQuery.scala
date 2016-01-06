package org.corespring.passage.search

import org.corespring.elasticsearch.ElasticSearchWrites
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.libs.json._

case class PassageIndexQuery(offset: Int = PassageIndexQuery.Defaults.offset,
  count: Int = PassageIndexQuery.Defaults.count,
  text: Option[String] = PassageIndexQuery.Defaults.text,
  contentTypes: Seq[String] = PassageIndexQuery.Defaults.contentTypes,
  collections: Seq[String] = PassageIndexQuery.Defaults.collections) {

  def scopedTo(identity: OrgAndOpts): PassageIndexQuery = {
    val accessible = identity.org.accessibleCollections.map(_.collectionId.toString)
    val filtered = this.collections.filter(id => accessible.contains(id))
    filtered.isEmpty match {
      case true => this.copy(collections = accessible)
      case _ => this.copy(collections = filtered)
    }
  }

}

object PassageIndexQuery {
  object Defaults {
    val count = 50
    val offset = 0
    val text = None
    val contentTypes = Seq.empty
    val collections = Seq.empty
  }

  object Fields {
    val text = "text"
    val contentTypes = "contentTypes"
    val collections = "collections"
  }

  object ApiReads extends Reads[PassageIndexQuery] {
    import Fields._

    override def reads(json: JsValue): JsResult[PassageIndexQuery] = JsSuccess(
      PassageIndexQuery(
        text = (json \ text).asOpt[String],
        contentTypes = (json \ contentTypes).asOpt[Seq[String]].getOrElse(PassageIndexQuery.Defaults.contentTypes),
        collections = (json \ collections).asOpt[Seq[String]].getOrElse(PassageIndexQuery.Defaults.collections))
    )
  }

  object ElasticSearchWrites extends ElasticSearchWrites[PassageIndexQuery] {
    override def writes(query: PassageIndexQuery): JsValue = {
      import query._

      val clauses = Seq(
        should(text, Seq("body"))
      ).flatten.foldLeft(Json.obj()) { case (obj, acc) => acc ++ obj }

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
              terms("contentType", contentTypes),
              terms("collectionId", collections)).flatten
            t
          })
        ))
      )
    }
  }

}