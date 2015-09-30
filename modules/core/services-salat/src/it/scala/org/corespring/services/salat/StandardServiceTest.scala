package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.models.{ Domain, StandardDomains, Standard }
import org.specs2.mutable.{ BeforeAfter, After }
import org.specs2.time.NoTimeConversions
import scala.concurrent.Await
import scala.concurrent.duration._

class StandardServiceTest extends ServicesSalatIntegrationTest with NoTimeConversions {

  "domains" should {

    trait scope extends BeforeAfter {

      def addStandard(subject: String, category: String, dotNotation: String): Standard = {
        val s: Standard = Standard(
          subject = Some(subject),
          category = Some(category),
          subCategory = Some(category),
          dotNotation = Some(dotNotation))
        val id = services.standardService.insert(s).get
        s.copy(id = id)
      }

      override def after: Any = {

      }

      override def before: Any = {
        import Standard.Subjects._
        addStandard(ELA, "ela-1", "C.1.2")
        addStandard(ELA, "ela-1", "C.1")
        addStandard(ELA, "ela-1", "C.1.1")
        addStandard(ELA, "ela-2", "C.2")
        println(s"Insertion complete")

        //val all = services.standardService.find(MongoDBObject.empty)
        //println(all)
      }
    }

    "return domains" in new scope {

      val expected = StandardDomains(
        Seq(
          Domain("ela-1", Seq("C.1", "C.1.1", "C.1.2")),
          Domain("ela-2", Seq("C.2"))),
        Seq.empty[Domain])

      val result = services.standardService.domains

      val inner = Await.result(result, 2.seconds)

      println(s"inner result : $inner")

      result must equalTo(expected).await(timeout = 10.seconds)
    }
  }
}
