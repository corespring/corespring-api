package org.corespring.models.auth

import org.specs2.mutable.Specification
import org.specs2.specification.Fragment

class PermissionTest extends Specification {

  "Permission" should {

    "has" should {

      def assertHas(a: Permission, b: Permission, has: Boolean): Fragment = {
        s"should return $has for ${a.name}.has(${b.name})" in { a.has(b) === has }
      }

      "write" should {
        assertHas(Permission.Write, Permission.Write, true)
        assertHas(Permission.Write, Permission.Read, true)
        assertHas(Permission.Write, Permission.Clone, true)
      }

      "clone" should {
        assertHas(Permission.Clone, Permission.Write, false)
        assertHas(Permission.Clone, Permission.Clone, true)
        assertHas(Permission.Clone, Permission.Read, true)
      }

      "read" should {
        assertHas(Permission.Read, Permission.Write, false)
        assertHas(Permission.Read, Permission.Clone, false)
        assertHas(Permission.Read, Permission.Read, true)
      }
    }

  }

}