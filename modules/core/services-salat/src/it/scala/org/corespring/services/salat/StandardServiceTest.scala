package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.models.{ Domain, StandardDomains, Standard }
import org.specs2.mutable.After

class StandardServiceTest extends ServicesSalatIntegrationTest {

  "domains" should {

    trait scope extends After {

      def addStandard(subject: String, category: String, dotNotation: String): Standard = {
        val s: Standard = Standard(
          subject = Some(subject),
          category = Some(category),
          subCategory = Some(category),
          dotNotation = Some(dotNotation))
        val id = services.standardService.insert(s).get
        s.copy(id = id)
      }

      import Standard.Subjects._
      val four = addStandard(ELA, "ela-1", "C.1.2")
      val one = addStandard(ELA, "ela-1", "C.1")
      val two = addStandard(ELA, "ela-1", "C.1.1")
      val three = addStandard(ELA, "ela-2", "C.2")

      override def after: Any = {

      }

    }

    "return domains" in new scope {

      val expected = StandardDomains(
        Seq(
          Domain("ela-1", Seq("C.1", "C.1.1", "C.1.2")),
          Domain("ela-2", Seq("C.2"))),
        Seq.empty[Domain])

      services.standardService.domains must equalTo(expected).await
    }
  }
}
