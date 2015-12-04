package org.corespring.salat.grater

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.novus.salat.Context
import org.corespring.models.item.resource.{Resource, StoredFile, VirtualFile}
import org.corespring.services.salat.ServicesContext
import org.specs2.mutable.Specification
import play.api.Play


class ResourceTest extends Specification {

   import play.api.Play.current


   implicit val context: Context = new ServicesContext(Play.classloader)

   "Resource" should {

     "support files" in {
       val dbo = MongoDBObject(
         "name" -> "test resource",
         "files" -> MongoDBList(
          MongoDBObject(
            "storageKey" -> "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg",
            "contentType" -> "image/jpg",
            "isMain" -> false,
            "name" -> "mc008-3.jpg"
          ),
           MongoDBObject(
             "content" -> "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<assessmentItem></assessmentItem>",
            "contentType" -> "text/xml",
             "isMain" -> true,
             "name" -> "qti.xml"
           )
         )
       )
       com.novus.salat.grater[Resource].asObject[Imports.DBObject](dbo) must_== Resource(
         name = "test resource",
         files = Seq(
           StoredFile("mc008-3.jpg", "image/jpg", false, "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg"),
           VirtualFile("qti.xml", "text/xml", true, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<assessmentItem></assessmentItem>"))
       )
     }
   }
 }
