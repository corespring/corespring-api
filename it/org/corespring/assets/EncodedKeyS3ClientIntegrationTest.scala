package org.corespring.assets

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import org.apache.commons.httpclient.util.URIUtil
import org.corespring.it.IntegrationSpecification
import org.specs2.mutable.After
import org.specs2.specification.{ Fragments, Step }

class EncodedKeyS3ClientIntegrationTest extends IntegrationSpecification {

  val rawS3 = new AmazonS3Client(main.awsCredentials)

  val client = main.s3

  val bucket = main.bucket.bucket

  trait scope extends After {
    lazy val stream = this.getClass.getResourceAsStream("/test-images/ervin.png")

    require(stream != null)

    val metadataUp = new ObjectMetadata()
    metadataUp.setContentType("image/png")
    def encode(s: String) = URIUtil.encodePath(s)
  }

  trait path extends scope {
    val sample = "raw^path/to a/ervin+ed.png"
    def mkS3Path(s: String): String
    def mkRequestPath(s: String): String
    lazy val s3Path = mkS3Path(sample)
    lazy val requestPath = mkRequestPath(sample)
  }

  trait get extends path {
    logger.info(s"[get] s3Path=$s3Path")
    rawS3.putObject(bucket, s3Path, stream, metadataUp)
    override def after: Any = client.deleteObject(bucket, s3Path)
  }

  trait getObject extends get {
    logger.info(s"[getObject] requestPath=$requestPath")
    lazy val obj = client.getObject(bucket, requestPath)
  }

  trait getObjectMetadata extends get {
    logger.info(s"[getObjectMetadata] requestPath=$requestPath")
    lazy val metadata = client.getObjectMetadata(bucket, requestPath)
  }

  "getObjectMetadata" should {

    "when calling with a plain path" should {

      trait plainPath extends getObjectMetadata {
        override def mkRequestPath(s: String) = s
      }

      "return the metadata at the encoded path if present" in new plainPath {
        override def mkS3Path(s: String): String = encode(s)
        metadata.getContentType must_== "image/png"
      }

      "return the metadata at the plain path if the encoded isn't present" in new plainPath {
        override def mkS3Path(s: String): String = s
        metadata.getContentType must_== "image/png"
      }
    }

    "when calling with an encoded path" should {

      trait encodedPath extends getObjectMetadata {
        override def mkRequestPath(s: String) = encode(s)
      }

      "return the metadata at the encoded path" in new encodedPath {
        override def mkS3Path(s: String): String = encode(s)
        metadata.getContentType must_== "image/png"
      }

      "return the metadata at the plain path if the encoded isn't present" in new encodedPath {
        override def mkS3Path(s: String): String = s
        metadata.getContentType must_== "image/png"
      }

    }
  }

  "getObject" should {

    "when calling with a plain path" should {

      trait plainPath extends getObject {
        override def mkRequestPath(s: String) = s
      }

      "return the object at the encoded path if present" in new plainPath {
        override def mkS3Path(s: String): String = encode(s)
        obj.getKey must_== s3Path
      }

      "return the object at the plain path if the encoded isn't present" in new plainPath {
        override def mkS3Path(s: String): String = s
        obj.getKey must_== sample
      }
    }

    "when calling with an encoded path" should {

      trait encodedPath extends getObject {
        override def mkRequestPath(s: String) = encode(s)
      }

      "return the object at the encoded path" in new encodedPath {
        override def mkS3Path(s: String): String = encode(s)
        obj.getKey must_== s3Path
      }

      "return the object at the plain path if the encoded isn't present" in new encodedPath {
        override def mkS3Path(s: String): String = s
        obj.getKey must_== sample
      }

    }
  }

  "putObject" should {
    trait putObject extends path {
      override def mkRequestPath(s: String): String = encode(s)
      logger.info(s"[putObject] s3Path=$s3Path")
      client.putObject(bucket, s3Path, stream, metadataUp)

      override def after: Any = client.deleteObject(bucket, s3Path)
    }

    "when the path is plain, put the object at the encoded path" in new putObject {
      override def mkS3Path(s: String): String = s
      rawS3.getObject(bucket, s3Path).getKey must_== s3Path
    }

    "when the path is encoded, put the object at the encoded path" in new putObject {
      override def mkS3Path(s: String): String = encode(s)
      rawS3.getObject(bucket, s3Path).getKey must_== s3Path
    }
  }

  "copyObject" should {

    trait copyObject extends path {

      val destination = "this is/a/path.png"

      override def mkRequestPath(s: String) = encode(s)
      rawS3.putObject(bucket, s3Path, stream, metadataUp)

      override def after: Any = client.deleteObject(bucket, s3Path)
    }

    "when copying with an encoded request path" should {

      trait copyObjectPlain extends copyObject {
        override def mkRequestPath(s: String) = encode(s)
      }

      "copy from encoded s3 path to an encoded destination" in new copyObjectPlain {
        override def mkS3Path(s: String) = encode(s)
        val result = client.copyObject(bucket, requestPath, bucket, destination)
        val newObject = client.getObject(bucket, destination)
        newObject.getKey must_== encode(destination)
      }

      "copy from a fallback plain s3 path to an encoded destination" in new copyObjectPlain {
        override def mkS3Path(s: String) = s
        val result = client.copyObject(bucket, requestPath, bucket, destination)
        val newObject = client.getObject(bucket, destination)
        newObject.getKey must_== encode(destination)
      }

    }

    "when copying with a plain request path" should {

      trait copyObjectPlain extends copyObject {
        override def mkRequestPath(s: String) = s
      }

      "copy from encoded s3 path to an encoded destination" in new copyObjectPlain {
        override def mkS3Path(s: String) = encode(s)
        val result = client.copyObject(bucket, requestPath, bucket, destination)
        val newObject = client.getObject(bucket, destination)
        newObject.getKey must_== encode(destination)
      }

      "copy from a fallback plain s3 path to an encoded destination" in new copyObjectPlain {
        override def mkS3Path(s: String) = s
        val result = client.copyObject(bucket, requestPath, bucket, destination)
        val newObject = client.getObject(bucket, destination)
        newObject.getKey must_== encode(destination)
      }
    }

    "deleteObject" should {

      trait deleteObject extends path {
        rawS3.putObject(bucket, s3Path, stream, metadataUp)

        override def after: Any = {
          //nothing to do
          Unit
        }
      }

      "delete the object at the encoded path" in new deleteObject {
        override def mkS3Path(s: String): String = encode(s)
        override def mkRequestPath(s: String): String = s
        client.deleteObject(bucket, requestPath) must not(throwA[Throwable])
      }

      "delete the object at the fallback plain path" in new deleteObject {
        override def mkS3Path(s: String): String = s
        override def mkRequestPath(s: String): String = s
        client.deleteObject(bucket, requestPath) must not(throwA[Throwable])
      }
    }

    "listObjects" should {

      trait listObjects extends scope {
        def encodePath: Boolean
        val s3Path = "pre fix/ervin.png"
        client.putObject(bucket, if (encodePath) encode(s3Path) else s3Path, stream, metadataUp)

        override def after: Any = client.deleteObject(bucket, s3Path)
      }

      "when listing objects from a plain s3 path" should {
        trait encodedlistObjects extends listObjects {
          override lazy val encodePath = false
        }

        "when called with encoded prefix" should {

          "return the encoded prefix in the object keys if the key is encoded" in new encodedlistObjects {
            client.getObject(bucket, encode(s3Path)).getKey must_== encode(s3Path)
            val listing = client.listObjects(bucket, encode("pre fix"))
            listing.getObjectSummaries.get(0).getKey must_== encode(s3Path)
          }
        }

        "when called with plain prefix" should {

          "return the plain prefix in the object keys if the key is encoded" in new encodedlistObjects {
            client.getObject(bucket, encode(s3Path)).getKey must_== encode(s3Path)
            val listing = client.listObjects(bucket, "pre fix")
            listing.getObjectSummaries.get(0).getKey must_== s3Path
          }
        }
      }

      "when listing objects from an encoded s3 path" should {
        trait encodedlistObjects extends listObjects {
          override lazy val encodePath = true
        }

        "when called with encoded prefix" should {

          "return the encoded prefix in the object keys if the key is encoded" in new encodedlistObjects {
            client.getObject(bucket, encode(s3Path)).getKey must_== encode(s3Path)
            val listing = client.listObjects(bucket, encode("pre fix"))
            listing.getObjectSummaries.get(0).getKey must_== encode(s3Path)
          }
        }

        "when called with plain prefix" should {

          "return the plain prefix in the object keys if the key is encoded" in new encodedlistObjects {
            client.getObject(bucket, encode(s3Path)).getKey must_== encode(s3Path)
            val listing = client.listObjects(bucket, "pre fix")
            listing.getObjectSummaries.get(0).getKey must_== s3Path
          }
        }
      }
    }
  }
}
