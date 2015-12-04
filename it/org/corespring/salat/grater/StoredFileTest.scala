package org.corespring.salat.grater

import com.novus.salat.Context
import org.corespring.models.item.resource.{StoredFile, VirtualFile}
import org.corespring.services.salat.ServicesContext
import org.specs2.mutable.Specification
import play.api.Play


class StoredFileTest extends Specification {

   import play.api.Play.current


   implicit val context: Context = new ServicesContext(Play.classloader)

   "StoredFile" should {

     "support stored files " in {
       com.novus.salat.grater[StoredFile].fromJSON("""{
         "storageKey": "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg",
         "contentType": "image/jpg",
         "isMain": false,
         "name": "mc008-3.jpg"
       }""") must_== StoredFile("mc008-3.jpg", "image/jpg", false, "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg")
     }
   }
 }
