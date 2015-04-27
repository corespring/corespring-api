package org.corespring.platform.core.models.item.index

import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ItemIndexSearchResultTest extends Specification {

  "Format" should {
    implicit val Format = ItemIndexSearchResult.Format

    val total = 100
    val id = new ObjectId().toString
    val collectionId = new ObjectId().toString
    val contributor = "CoreSpring"
    val published = true
    val standards = Map("RL.4.3" -> "Describe in depth a character, setting, or event in a story or drama, drawing on specific details in the text (e.g., a character’s thoughts, words, or actions).")
    object primarySubject {
      val category = "Algebra"
      val subject = "Mathematics"
    }
    val gradeLevels = Seq("09", "10", "11", "12")
    val title = "This is the title"
    val description = "This is the description"
    val apiVersion = 1
    val itemTypes = Seq("corespring-multiple-choice", "corespring-feedback")

    val searchResult = ItemIndexSearchResult(
      total = total,
      hits = Seq(ItemIndexHit(
        id = id,
        collectionId = collectionId,
        contributor = Some(contributor),
        published = published,
        standards = standards,
        subject = Some(s"${primarySubject.category}: ${primarySubject.subject}"),
        gradeLevels = gradeLevels,
        title = Some(title),
        description = Some(description),
        apiVersion = Some(apiVersion),
        itemTypes = itemTypes
      ))
    )

    "reads" should {

      val json = Json.parse(s"""{
        "hits" : {
          "total" : $total,
          "hits" : [{
            "_id": "$id",
            "_source": {
              "apiVersion" : 1,
              "collectionId": "$collectionId",
              "contributorDetails" : {
                "contributor": "$contributor"
              },
              "published" : $published,
              "standards" : [${standards.map{case (dotNotation, standard) => Json.obj("dotNotation" -> dotNotation, "standard" -> standard)}.mkString("\",\"")}],
              "taskInfo" : {
                "subjects" : {
                  "primary" : {
                    "category" : "${primarySubject.category}",
                    "subject": "${primarySubject.subject}"
                  }
                },
                "gradeLevel" : ["${gradeLevels.mkString("\",\"")}"],
                "title" : "$title",
                "itemTypes": ["${itemTypes.mkString("\",\"")}"],
                "description": "$description"
              }
            }
          }]
        }
      }""")

      val result = Json.fromJson[ItemIndexSearchResult](json).getOrElse(throw new Exception("Couldn't parse JSON"))

      "read total" in {
        result.total must be equalTo(total)
      }

      "read hits" in {
        result.hits.headOption match {
          case Some(resultHit) => resultHit === searchResult.hits.head
          case _ => failure("No results parsed from JSON")
        }
      }

    }

    "writes" should {

      "write to flattened JSON" in {
        val result = Json.toJson(searchResult)
        result === Json.parse(s"""{
          "total" : $total,
          "hits" : [{
            "id" : "$id",
            "collectionId" : "$collectionId",
            "contributor": "$contributor",
            "published" : $published,
            "standards" : ${Json.toJson(standards).toString},
            "subject" : "${primarySubject.category}: ${primarySubject.subject}",
            "gradeLevels" : ["${gradeLevels.mkString("\",\"")}"],
            "title" : "$title",
            "description" : "$description",
            "apiVersion" : $apiVersion,
            "itemTypes" : ["${itemTypes.mkString("\",\"")}"]
          }]
        }""")
      }

    }

  }

}