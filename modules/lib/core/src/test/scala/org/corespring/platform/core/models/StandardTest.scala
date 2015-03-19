package org.corespring.platform.core.models

import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification

class StandardTest extends Specification {

  import Standard.Subjects._

  PlaySingleton.start()

  "domain" should {

    val category = Some("this is the category")
    val subCategory = Some("and this is the subCategory")

    def standardForSubject(subject: String) =
      Standard(subject = Some(subject), category = category, subCategory = subCategory)

    "equal subCategory if subject is 'ELA-Literacy'" in {
      standardForSubject(ELALiteracy).domain must be equalTo(subCategory)
    }

    "equal subCategory if subject is 'ELA'" in {
      standardForSubject(ELA).domain must be equalTo(subCategory)
    }

    "equal category if subject is 'Math'" in {
      standardForSubject(Math).domain must be equalTo(category)
    }

    "equal None if subject is something other than ELA/Math" in {
      standardForSubject("Science").domain must be equalTo(None)
    }

  }

}
