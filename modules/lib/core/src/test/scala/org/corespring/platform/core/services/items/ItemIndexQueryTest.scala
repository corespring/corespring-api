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
      default.offset must be equalTo(Defaults.offset)
      default.count must be equalTo(Defaults.count)
      default.text must be equalTo(Defaults.text)
      default.contributors must be equalTo(Defaults.contributors)
      default.collections must be equalTo(Defaults.collections)
      default.itemTypes must be equalTo(Defaults.itemTypes)
      default.gradeLevels must be equalTo(Defaults.gradeLevels)
      default.published must be equalTo(Defaults.published)
      default.workflows must be equalTo(Defaults.workflows)
    }
  }

  "ApiReads" should {
    implicit val ApiReads = ItemIndexQuery.ApiReads

    val offset = 10
    val count = 10
    val text = "hey this is some text for the query"
    val contributors = Seq("these", "are", "contributors")
    val collections = (1 to 5).map(f => new ObjectId().toString)
    val itemTypes = Seq("Multiple Choice", "Short Answer - Fill In The Blank")
    val gradeLevels = Seq("09", "10", "11", "12")
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
      "workflows" : ["${workflows.mkString("\",\"")}"]
    }"""

    "fromJson" should {
      val query = Json.fromJson[ItemIndexQuery](Json.parse(apiJson)).getOrElse(throw new Exception("Couldn't parse JSON"))

      "set fields on ItemIndexQuery" in {
        query.offset must be equalTo(offset)
        query.count must be equalTo(count)
        query.text must be equalTo(Some(text))
        query.contributors must be equalTo(contributors)
        query.collections must be equalTo(collections)
        query.itemTypes must be equalTo(itemTypes)
        query.gradeLevels must be equalTo(gradeLevels)
        query.published must be equalTo(Some(published))
        query.workflows must be equalTo(workflows)
      }

    }

  }

  "ElasticSearchWrites" should {
    implicit val ElasticSearchWrites = ItemIndexQuery.ElasticSearchWrites

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

        "include query as multi_match query parameter" in {
          (json \ "query" \ "multi_match" \ "query") must be equalTo (JsString(text))
        }
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
      Seq("term", "terms").map(key => (filters.find(f => (f \ key \ filter) match {
        case _: JsUndefined => false
        case _ => true
      }))).flatten.headOption
    }

    def hasFilter(filter: String) = getFilter(filter).nonEmpty

    private def hasFilter[T](filter: String, value: T, filterName: String, execution: Option[String])
                            (implicit reads: Reads[T]) = {
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

  }

}
