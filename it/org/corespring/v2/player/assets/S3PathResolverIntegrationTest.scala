package org.corespring.v2.player.assets

import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.specs2.mutable.BeforeAfter

class S3PathResolverIntegrationTest extends IntegrationSpecification {

  trait scope extends BeforeAfter {

    def path = "/test-images/ervin.png"
    def key: String
    val resolver = main.s3PathResolver

    override def before = {
      val img = ImageUtils.resourcePathToFile(path)
      println(s"uploading: $key")
      ImageUtils.upload(img, key)
    }

    override def after = {
      println(s"deleting: $key")
      ImageUtils.delete(key)
    }
  }

  "resolve" should {

    "find the asset" in new scope {
      override def key: String = "blah/blah.png"
      resolver.resolve("blah", "/blah.png") must_== Seq(key)
    }

    "find the asset nested a few levels in" in new scope {
      override def key: String = "blah/dir-one/dir-two/blah.png"
      resolver.resolve("blah", ".*/dir-one/.*?/blah.png") must_== Seq(key)
    }
  }
}
