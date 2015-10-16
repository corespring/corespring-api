package org.corespring.hash

import org.specs2.mutable.Specification

class BCryptHasherTest extends Specification {

  "hash" should {
    "hash a password and match it the same instances" in {
      val hasher = new BCryptHasher()
      val hashed = hasher.hash("hi")
      hasher.matches(hashed, "hi") === true
    }

    "hash a password and match it using different instances" in {
      val hasherOne = new BCryptHasher()
      val hashed = hasherOne.hash("hi")
      val hasherTwo = new BCryptHasher()
      hasherTwo.matches(hashed, "hi") === true
    }

    "hash a password and fail to match it" in {
      val hasher = new BCryptHasher()
      val hashed = hasher.hash("hi")
      hasher.matches(hashed, "hi!") === false
    }
  }
}
