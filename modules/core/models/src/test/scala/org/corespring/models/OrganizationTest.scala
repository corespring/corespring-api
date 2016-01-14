package org.corespring.models

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.specs2.mutable.Specification

class OrganizationTest extends Specification {

  def org(refs: ContentCollRef*) = {
    Organization("test", contentcolls = refs)
  }

  def ref(enabled: Boolean, p: Permission = Permission.Read) = {
    ContentCollRef(ObjectId.get, p.value, enabled)
  }

  "accessibleCollections" should {

    "return an empty Seq if all the refs are disabled" in {
      org(ref(false)).accessibleCollections must_== Seq.empty
    }

    "not return disabled refs" in {
      val refOne = ref(false)
      org(refOne).accessibleCollections must_== Seq.empty
    }

    "return 1 read ref" in {
      val refOne = ref(true)
      org(refOne).accessibleCollections must_== Seq(refOne)
    }

    "return 2 read refs" in {
      val refOne = ref(true)
      val refTwo = ref(true)
      org(refOne, refTwo).accessibleCollections must_== Seq(refOne, refTwo)
    }

    "return 1 read, 1 clone refs" in {
      val refOne = ref(true)
      val refTwo = ref(true, Permission.Clone)
      org(refOne, refTwo).accessibleCollections must_== Seq(refOne, refTwo)
    }

    "return 1 read, 1 write refs" in {
      val refOne = ref(true)
      val refTwo = ref(true, Permission.Write)
      org(refOne, refTwo).accessibleCollections must_== Seq(refOne, refTwo)
    }

    "return 1 clone, 1 write refs" in {
      val refOne = ref(true, Permission.Clone)
      val refTwo = ref(true, Permission.Write)
      org(refOne, refTwo).accessibleCollections must_== Seq(refOne, refTwo)
    }
  }

}
