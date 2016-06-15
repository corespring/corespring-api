package org.corespring.web.common.controllers.deployment

import org.specs2.mutable.Specification

class ValidBucketNameTest extends Specification {

  "bucket" should {

    "remove hotfix" in {
      ValidBucketName("", "hotfix/branch") must_== s"${ValidBucketName.base}-branch"
    }

    "remove hotfix-" in {
      ValidBucketName("", "hotfix-branch") must_== s"${ValidBucketName.base}-branch"
    }

    "remove feature" in {
      ValidBucketName("", "feature/branch") must_== s"${ValidBucketName.base}-branch"
    }

    "remove feature-" in {
      ValidBucketName("", "feature-branch") must_== s"${ValidBucketName.base}-branch"
    }

    "handle /branch" in {
      ValidBucketName("", "/branch") must_== s"${ValidBucketName.base}-branch"
    }

    "handle env and branch" in {
      ValidBucketName("env", "/branch") must_== s"${ValidBucketName.base}-env-branch"
    }
  }

}
