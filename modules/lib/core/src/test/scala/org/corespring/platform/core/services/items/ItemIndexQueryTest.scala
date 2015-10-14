package org.corespring.platform.core.services.items

import org.bson.types.ObjectId
import org.corespring.platform.core.services.item.ItemIndexQuery
import org.specs2.mutable.Specification
import play.api.libs.json._

class ItemIndexQueryTest extends Specification {

  "defaults" should {
    import ItemIndexQuery.Defaults

    "read from ItemIndexQuery.Defaults" in {
      val default = ItemIndexQuery()
      default.offset must be equalTo (Defaults.offset)
      default.count must be equalTo (Defaults.count)
      default.text must be equalTo (Defaults.text)
      default.contributors must be equalTo (Defaults.contributors)
      default.collections must be equalTo (Defaults.collections)
      default.itemTypes must be equalTo (Defaults.itemTypes)
      default.gradeLevels must be equalTo (Defaults.gradeLevels)
      default.published must be equalTo (Defaults.published)
      default.workflows must be equalTo (Defaults.workflows)
      default.requiredPlayerWidth must be equalTo (Defaults.requiredPlayerWidth)
    }
  }

  "ApiReads" should {
    implicit val ApiReads = ItemIndexQuery.ApiReads

    val offset = 10
    val count = 10
    val requiredPlayerWidth = 500
    val text = "hey this is some text for the query"
    val contributors = Seq("these", "are", "contributors")
    val collections = (1 to 5).map(f => new ObjectId().toString)
    val itemTypes = Seq("Multiple Choice", "Short Answer - Fill In The Blank")
    val gradeLevels = Seq("09", "10", "11", "12")
    val standards = Seq("RI.7.1", "W.7.1b")
    val published = true
    val workflows = Seq("qaReview")

    val apiJson = s"""{
      "offset" : $offset,
      "count" : $count,
      "text": "$text",
      "contributors": ["${contributors.mkString("\",\"")}"],
      "collections": ["${collections.mkString("\",\"")}"],
      "itemTypes": ["${itemTypes.mkString("\",\"")}"],
      "gradeLevels": ["${gradeLevels.mkString("\",\"")}"],
      "published" : $published,
      "standards" : ["${standards.mkString("\",\"")}"],
      "requiredPlayerWidth" : $requiredPlayerWidth,
      "workflows" : ["${workflows.mkString("\",\"")}"]
    }"""

    "fromJson" should {
      val query = Json.fromJson[ItemIndexQuery](Json.parse(apiJson)).getOrElse(throw new Exception("Couldn't parse JSON"))

      "set fields on ItemIndexQuery" in {
        query.offset must be equalTo (offset)
        query.count must be equalTo (count)
        query.text must be equalTo (Some(text))
        query.contributors must be equalTo (contributors)
        query.collections must be equalTo (collections)
        query.itemTypes must be equalTo (itemTypes)
        query.gradeLevels must be equalTo (gradeLevels)
        query.published must be equalTo (Some(published))
        query.standards must be equalTo(standards)
        query.workflows must be equalTo (workflows)
        query.requiredPlayerWidth must be equalTo (Some(requiredPlayerWidth))
      }

    }

  }

  "ElasticSearchWrites" should {
    implicit val ElasticSearchWrites = ItemIndexQuery.ElasticSearchWrites

    def shouldClause(json: JsValue, name: String) = clause(json, name, "should")
    def mustClause(json: JsValue, name: String) = clause(json, name, "must")

    def clause(json: JsValue, name: String, clauseName: String) =
      (json \ "query" \ "bool" \ clauseName).as[Seq[JsObject]]
        .find(node => (node \ name).asOpt[JsObject].nonEmpty).map(_ \ name)
        .getOrElse(JsUndefined(s"Could not find $name clause"))

    def multiMatch(json: JsValue): JsValue = shouldClause(json, "multi_match")
    def nested(json: JsValue): JsValue = mustClause(json, "nested")
    def ids(json: JsValue): JsValue = shouldClause(json, "ids")

    "text" should {
      "blank" should {
        val query = ItemIndexQuery(text = None)
        val json = Json.toJson(query)

        "not include query" in {
          (json \ "query") must beAnInstanceOf[JsUndefined]
        }
      }

      "not blank" should {
        val text = "Hey this is my query!"
        val query = ItemIndexQuery(text = Some(text))
        val json = Json.toJson(query)

        "multi_match" should {

          "include query as query parameter" in {
            (multiMatch(json) \ "query").as[String] must be equalTo (text)
          }

          "query on title field" in {
            (multiMatch(json) \ "fields").as[Seq[String]] must contain("taskInfo.title")
          }

          "query on description field" in {
            (multiMatch(json) \ "fields").as[Seq[String]] must contain("taskInfo.description")
          }

          "query on content" in {
            (multiMatch(json) \ "fields").as[Seq[String]] must contain("content")
          }

          "query on standard" in {
            (multiMatch(json) \ "fields").as[Seq[String]] must contain("standards.dotNotation")
          }

        }

        "include ids querying" in {
          (ids(json)) must haveClass[JsObject]
          (ids(json) \ "values").as[Seq[String]].headOption must be equalTo (Some(text))
        }

      }
    }

    "metadata" should {
      val metadata = Map("kds.scoringType" -> "PARCC")
      val query = ItemIndexQuery(metadata = metadata)
      val json = Json.toJson(query)

      "include nested metadata query" in {
        (nested(json) \ "path").as[String] must be equalTo("metadata")
      }

      "matches on metadata.key and metadata.value" in {
        val mustClauses = (nested(json) \ "query" \ "bool" \ "must").as[Seq[JsObject]]
        mustClauses.find(c => (c \ "match").as[JsObject].keys.contains("metadata.key")) must not beEmpty;
        mustClauses.find(c => (c \ "match").as[JsObject].keys.contains("metadata.value")) must not beEmpty
      }

    }


    "contributors" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(contributors = Seq.empty)).hasFilter("contributorDetails.contributor") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as terms filter" in {
          val contributors = Seq("CoreSpring")
          Json.toJson(ItemIndexQuery(contributors = contributors))
            .hasTermsFilter("contributorDetails.contributor", contributors) must beTrue
        }
      }

    }

    "collections" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(collections = Seq.empty)).hasFilter("collectionId") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as terms filter" in {
          val collections = Seq(new ObjectId().toString)
          Json.toJson(ItemIndexQuery(collections = collections)).hasTermsFilter("collectionId", collections) must beTrue
        }
      }

    }

    "itemTypes" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(itemTypes = Seq.empty)).hasFilter("taskInfo.itemTypes") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as terms filter" in {
          val itemTypes = Seq("Multiple Choice", "Short Answer - Fill In The Blank")
          Json.toJson(ItemIndexQuery(itemTypes = itemTypes)).hasTermsFilter("taskInfo.itemTypes", itemTypes) must beTrue
        }
      }

    }

    "gradeLevels" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(gradeLevels = Seq.empty)).hasFilter("taskInfo.gradeLevel") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as terms filter" in {
          val gradeLevels = Seq("09", "10", "11", "12")
          Json.toJson(ItemIndexQuery(gradeLevels = gradeLevels))
            .hasTermsFilter("taskInfo.gradeLevel", gradeLevels) must beTrue
        }
      }

    }

    "published" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(published = None)).hasFilter("published") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as term filter" in {
          val published = false
          Json.toJson(ItemIndexQuery(published = Some(published)))
            .hasTermFilter("published", published) must beTrue
        }
      }

    }

    "standards" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(standards = Seq.empty)).hasFilter("standards.dotNotation") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as terms filter" in {
          val standards = Seq("RI.7.1", "W.7.1b")
          Json.toJson(ItemIndexQuery(standards = standards)).hasTermsFilter("standards.dotNotation", standards) must beTrue
        }
      }

    }

    "requiredPlayerWidth" should {
      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(requiredPlayerWidth = None)).hasFilter("requiredPlayerWidth") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as range filter" in {
          val requiredPlayerWidth = 500
          Json.toJson(ItemIndexQuery(requiredPlayerWidth = Some(requiredPlayerWidth)))
          .hasRangeFilter("minimumWidth", lte = Some(500)) must beTrue
        }
      }
    }

    "workflows" should {

      "empty" should {
        "not be included in filter" in {
          Json.toJson(ItemIndexQuery(workflows = Seq.empty)).hasFilter("workflow") must beFalse
        }
      }

      "nonEmpty" should {
        "be included as terms filter with 'and' execution" in {
          val workflows = Seq("qaReview")
          Json.toJson(ItemIndexQuery(workflows = workflows))
            .hasTermsFilter("workflow", workflows, Some("and")) must beTrue
        }
      }

    }

  }

  private implicit class WithFilterOptions(json: JsValue) {

    private def getFilter(filter: String): Option[JsObject] = {
      val filters = (json \ "filter" \ "bool" \ "must").asOpt[Seq[JsObject]].getOrElse(Seq.empty)
      Seq("term", "terms","range").map(key => (filters.find(f => (f \ key \ filter) match {
        case _: JsUndefined => false
        case _ => true
      }))).flatten.headOption
    }

    def hasFilter(filter: String) = getFilter(filter).nonEmpty

    private def hasFilter[T](filter: String, value: T, filterName: String, execution: Option[String])(implicit reads: Reads[T]) = {
      val f = getFilter(filter).get
      val matchesValue = (f \ filterName \ filter).as[T] == value
      execution match {
        case None => matchesValue
        case Some(execution) => matchesValue && (f \ filterName \ "execution").as[String] == execution
      }
    }

    def hasTermsFilter[T](filter: String, value: T, execution: Option[String] = None)(implicit reads: Reads[T]) =
      hasFilter[T](filter, value, "terms", execution)

    def hasTermFilter[T](filter: String, value: T, execution: Option[String] = None)(implicit reads: Reads[T]) =
      hasFilter[T](filter, value, "term", execution)

    def hasRangeFilter(filter: String, lte: Option[Int] = None, lt: Option[Int] = None, gte: Option[Int] = None, gt: Option[Int] = None) = {
      val f = getFilter(filter).get
      (f \ "range" \ filter \ "lte").asOpt[Int] == lte
      (f \ "range" \ filter \ "lt").asOpt[Int] == lt
      (f \ "range" \ filter \ "gte").asOpt[Int] == gte
      (f \ "range" \ filter \ "gt").asOpt[Int] == gt
    }

  }

}
