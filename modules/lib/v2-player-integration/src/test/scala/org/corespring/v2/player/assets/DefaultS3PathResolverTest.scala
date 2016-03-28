package org.corespring.v2.player.assets

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ ListObjectsRequest, ObjectListing, S3ObjectSummary }
import org.corespring.models.appConfig.Bucket
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class DefaultS3PathResolverTest extends Specification with Mockito {

  trait scope extends Scope {

    def keys: Seq[String] = Nil

    lazy val s3 = {
      val m = mock[AmazonS3]
      m.listObjects(any[ListObjectsRequest]) returns {
        val ol = mock[ObjectListing]
        import scala.collection.JavaConversions._
        ol.getObjectSummaries.returns {
          keys.map { k =>
            val os = mock[S3ObjectSummary]
            os.getKey returns k
            os
          }.toList
        }
        ol
      }
      m
    }

    lazy val bucket = Bucket("bucket")

    val resolver = new DefaultS3PathResolver(
      s3,
      bucket)
  }

  "resolve" should {

    "returns no paths for no keys" in new scope {
      resolver.resolve("prefix", "key") must_== Nil
    }

    "returns one path" in new scope {
      override lazy val keys = Seq("prefix/key")
      resolver.resolve("prefix", "/key") must_== keys
      resolver.resolve("prefix", ".*/key") must_== keys
    }

    "returns one path for a regex string" in new scope {
      override lazy val keys = Seq("prefix/a/b/c/d/key")
      resolver.resolve("prefix", ".*/key") must_== keys
    }

    "returns zero paths for a bad regex string" in new scope {
      override lazy val keys = Seq("prefix/a/b/c/d/key")
      resolver.resolve("prefix", "b/a/.*/key") must_== Nil
    }
  }

}
