package org.corespring.platform.core.services.item

import java.io.{ InputStream, ByteArrayInputStream }

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class SupportingMaterialsAssetsTest extends Specification with Mockito {

  val resource = Resource(name = "material", files = Seq.empty)
  val file = StoredFile("image.png", "image/png", false)

  class scope extends Scope {

    val mockS3 = {
      val m = mock[AmazonS3]

      m.listObjects(any[String], any[String]) returns {
        val o = mock[ObjectListing]
        val arrayList = new java.util.ArrayList[S3ObjectSummary]()
        val mockSummary = new S3ObjectSummary()
        mockSummary.setKey("object-summary-key")
        arrayList.add(mockSummary)
        o.getObjectSummaries returns arrayList
        o
      }
      m
    }

    val mockKeys = {
      val m = mock[AssetKeys[String]]
      m.supportingMaterialFolder(any[String], any[String]) returns "asset-key"
      m.supportingMaterialFile(any[String], any[String], any[String]) returns "asset-key-file"
      m
    }

    val assets = new SupportingMaterialsAssets[String](mockS3, "bucket", mockKeys)
  }

  "deleteDir" should {
    "call s3.listObjects" in new scope {
      assets.deleteDir("id", resource)
      there was one(mockS3).listObjects("bucket", "asset-key")
    }

    "call s3.deleteObjects" in new scope {
      assets.deleteDir("id", resource)
      there was one(mockS3).deleteObjects(any[DeleteObjectsRequest])
    }

    "return the resource" in new scope {
      val result = assets.deleteDir("id", resource)
      result must_== Success(resource)
    }
  }

  "getS3Object" should {
    "call s3.getObject" in new scope {
      assets.getS3Object("id", resource.name, "file", None)
      there was one(mockS3).getObject("bucket", "asset-key-file")
    }
  }

  "upload" should {

    class uploadScope extends scope {
      assets.upload("id", resource, file, Array.empty)
      val bucketCaptor = capture[String]
      val keyCaptor = capture[String]
      val metadataCaptor = capture[ObjectMetadata]
      there was one(mockS3).putObject(bucketCaptor, keyCaptor, any[ByteArrayInputStream], metadataCaptor)
    }

    "call s3.putObject" in new uploadScope

    "set the bucket" in new uploadScope {
      bucketCaptor.value === "bucket"
    }

    "set the key" in new uploadScope {
      keyCaptor.value === "asset-key-file"
    }

    "set the metadata" in new uploadScope {
      metadataCaptor.value.getContentLength === 0
      metadataCaptor.value.getContentType === "image/png"
    }

    "an s3 error returns a Failure" in new scope {
      mockS3.putObject(any[String], any[String], any[InputStream], any[ObjectMetadata]).throws {
        new RuntimeException("S3-Error")
      }
      assets.upload("id", resource, file, Array.empty) must_== Failure("An error occurred: S3-Error")
    }
  }

  "deleteFile" should {
    "call s3.deleteObject" in new scope {
      assets.deleteFile("id", resource, "file")
      there was one(mockS3).deleteObject("bucket", "asset-key-file")
    }
  }
}
