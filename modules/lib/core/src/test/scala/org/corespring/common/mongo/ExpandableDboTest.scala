package org.corespring.common.mongo

import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
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

    "a.b returns an object" in {
      dbo.expandPath("a.b") === Some(MongoDBObject("c" -> "c-result"))
    }

    "a.b.c returns a string" in {
      dbo.expandPath("a.b.c") === Some("c-result")
    }

    "d.e.f returns object" in {
      dbo.expandPath("d.e.f") === Some(MongoDBObject("key" -> "value"))
    }

    "a.b.z returns None" in {
      dbo.expandPath("a.b.z") === None
    }

    "g.0 returns object" in {
      dbo.expandPath("g.0") === Some(MongoDBObject("h" -> "h-result"))
    }

    "g.1 returns object" in {
      dbo.expandPath("g.1") === Some(MongoDBObject("i" -> "i-result"))
    }

    "j.0.0 returns object" in {
      dbo.expandPath("j.0.0") === Some(MongoDBObject("j-key" -> "j-value"))
    }

    "j.0 returns list" in {
      dbo.expandPath("j.0") === Some(MongoDBList(MongoDBObject("j-key" -> "j-value")))
    }

    "j.0.0.j-key returns string" in {
      dbo.expandPath("j.0.0.j-key") === Some("j-value")
    }

    "work with core mongo types" in {
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
