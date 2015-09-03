package org.corespring.common.mongo

import com.mongodb.casbah.Imports._
import com.mongodb.{ BasicDBList, BasicDBObject, DBObject }
import org.specs2.mutable.Specification

class ExpandableDboTest extends Specification {

  import ExpandableDbo._

  "expandPath" should {

    val json =
      """
        {
          "a" : {
            "b" : {
              "c" : "c-result"
            },
          },
          "d" : {
            "e" : {
               "f" : { "key" : "value" }
            }
          },
          "g" : [
            { "h" : "h-result" },
            { "i" : "i-result" }
          ],
          "j" : [
            [
             { "j-key" : "j-value" }
            ]
          ]

        }
      """.stripMargin

    val dbo = com.mongodb.util.JSON.parse(json).asInstanceOf[DBObject]

    "casbah 'expand' fails when trying to expand against a list" in {
      dbo.expand[DBObject]("j.0.0") must throwA[ClassCastException]
    }

    "2.levels - returns an object" in {
      dbo.expandPath("a.b") === Some(MongoDBObject("c" -> "c-result"))
    }

    "3 levels - returns an object" in {
      dbo.expandPath("d.e.f") === Some(MongoDBObject("key" -> "value"))
    }

    "bad path - returns None" in {
      dbo.expandPath("a.b.z") === None
    }

    "2 levels with first child in a list - returns object" in {
      dbo.expandPath("g.0") === Some(MongoDBObject("h" -> "h-result"))
    }

    "2 levels with 2nd child in a list - returns object" in {
      dbo.expandPath("g.1") === Some(MongoDBObject("i" -> "i-result"))
    }

    "2 levels index out of bounds" in {
      dbo.expandPath("g.2") === None
    }

    "3 levels, 2 indices - returns an object" in {
      dbo.expandPath("j.0.0") === Some(MongoDBObject("j-key" -> "j-value"))
    }

    "2 levels, 1 index - returns a list" in {
      dbo.expandPath("j.0") === Some(MongoDBList(MongoDBObject("j-key" -> "j-value")).underlying)
    }

    "supporting materials sample" in {
      val path = "supportingMaterials.0.files"
      val json =
        """{
          |"supportingMaterials" : [
          |  { "files" : [
          |     { "_t" : "org.corespring.platform.core.models.item.resource.VirtualFile" ,
          |       "name" : "index.html" ,
          |       "contentType" : "text/html" ,
          |       "isMain" : true ,
          |       "content" : "old content"}
          |     ]
          |   }
          | ]
          |} """.stripMargin

      val dbo = com.mongodb.util.JSON.parse(json).asInstanceOf[DBObject]
      dbo.expandPath(path).map { l =>

        println(l)
        l.asInstanceOf[BasicDBList]
        success
      }.getOrElse(failure("should have got an item"))
    }

    "core mongo types are navigable" in {
      val name = new BasicDBObject()
      name.put("name", "ed")
      val details = new BasicDBObject()
      details.put("details", name)
      val l = new BasicDBList()
      l.add(details)
      val root = new BasicDBObject()
      root.put("a", l)
      root.expandPath("a.0.details") === Some(MongoDBObject("name" -> "ed"))
    }
  }
}
