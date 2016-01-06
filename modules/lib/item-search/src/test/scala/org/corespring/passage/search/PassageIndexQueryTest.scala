package org.corespring.passage.search

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ContentCollRef, Organization}
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{PlayerAccessSettings, OrgAndOpts}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class PassageIndexQueryTest extends Specification with Mockito {

  "scopedTo" should {

    trait ScopedTo extends Scope {
      val accessibleCollections = List.fill(3)(ContentCollRef(new ObjectId(), Permission.Read.value, true))
      val organization = Organization("", contentcolls = accessibleCollections)
      val identity = OrgAndOpts(organization, mock[PlayerAccessSettings], mock[AuthMode], None)

    }

    "with no provided collections" should {

      val passageIndexQuery = PassageIndexQuery()

      "add collections from identity" in new ScopedTo {
        passageIndexQuery.scopedTo(identity).collections must be equalTo accessibleCollections.map(_.collectionId.toString)
      }

    }

  }

}
