package org.corespring.salat.grater

import com.novus.salat.Context
import org.corespring.models.item.resource.{StoredFile, VirtualFile}
import org.corespring.services.salat.ServicesContext
import org.specs2.mutable.Specification
import play.api.Play


class VirtualFileTest extends Specification {

   import play.api.Play.current


   implicit val context: Context = new ServicesContext(Play.classloader)

   "VirtualFile" should {

     "support virtual files" in {
       com.novus.salat.grater[VirtualFile].fromJSON("""
        {
          "content": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<assessmentItem></assessmentItem>",
          "contentType": "text/xml",
          "isMain": true,
          "name": "qti.xml"
        }
       """) must_== VirtualFile("qti.xml", "text/xml", true, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<assessmentItem></assessmentItem>")
     }
   }
 }
