package org.corespring.v2.player

import java.util.Date

import org.joda.time.DateTime
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class SignedItemAssetResolverTest extends Specification with Mockito {

  "SignedItemAssetResolver" should {

    trait scope extends Scope {
      val validDomain = Some("//domain")
      val invalidDomain = Some("invalid")
      val emptyDomain = None
      val emptyVersion = None
      val version = Some("1234")

      val urlSigner = {
        val m = mock[CdnUrlSigner]
        m.signUrl(any[String], any[Date]) returns "fake signed url"
        m
      }
    }

    def beCloseInTimeTo(date: Date, timeDiff: Int = 500) = new Matcher[Date] {
      def apply[D <: Date](e: Expectable[D]) =
        result((e.value.getTime - date.getTime) < timeDiff,
          "Dates are nearly at the same time",
          "Dates are different",
          e)
    }


    "on creation" should {

      "throw error when cdnDomain is not set" in new scope {
        new SignedItemAssetResolver(emptyDomain, 24, urlSigner, version) should throwA[IllegalArgumentException]
      }

      "throw error when cdnDomain is not valid" in new scope {
        new SignedItemAssetResolver(invalidDomain, 24, urlSigner, version) should throwA[IllegalArgumentException]
      }

      "throw error when validInHours is <= 0" in new scope {
        new SignedItemAssetResolver(validDomain, 0, urlSigner, version) should throwA[IllegalArgumentException]
      }

      "not throw when all parameters are set" in new scope {
        new SignedItemAssetResolver(validDomain, 24, urlSigner, version) should haveClass[SignedItemAssetResolver]
      }
    }

    "on calling resolve" should {

      "return signed url" in new scope {
        val sut = new SignedItemAssetResolver(validDomain, 24, urlSigner, version)
        sut.resolve("123456789012345678901234:0")("test.jpeg") === "fake signed url"
      }

      "append version" in new scope {
        val sut = new SignedItemAssetResolver(validDomain, 24, urlSigner, version)
        sut.resolve("123456789012345678901234:0")("test.jpeg")
        there was one(urlSigner).signUrl(find("version=1234"), any[Date])
      }

      "not append version if it is None" in new scope {
        val sut = new SignedItemAssetResolver(validDomain, 24, urlSigner, version = None)
        sut.resolve("123456789012345678901234:0")("test.jpeg")
        there was one(urlSigner).signUrl(not(find("version=1234")), any[Date])
      }

      "use domain" in new scope {
        val sut = new SignedItemAssetResolver(validDomain, 24, urlSigner, version)
        sut.resolve("123456789012345678901234:0")("test.jpeg")
        there was one(urlSigner).signUrl(find("://domain/"), any[Date])
      }

      "use https protocol" in new scope {
        val sut = new SignedItemAssetResolver(validDomain, 24, urlSigner, version)
        sut.resolve("123456789012345678901234:0")("test.jpeg")
        there was one(urlSigner).signUrl(find("https://"), any[Date])
      }

      "use correct s3 path" in new scope {
        val sut = new SignedItemAssetResolver(validDomain, 24, urlSigner, version)
        val itemId = "123456789012345678901234"
        val itemVersion = 0
        val file = "test.jpeg"
        sut.resolve(itemId + ":" + itemVersion)(file)
        there was one(urlSigner).signUrl(find("/" + itemId + "/" + itemVersion + "/data/" + file), any[Date])
      }

      "pass correct valid-until-date" in new scope {
        val durationInHours = 24
        val expectedDate = DateTime.now().plusHours(durationInHours).toDate
        val sut = new SignedItemAssetResolver(validDomain, durationInHours, urlSigner, version)
        sut.resolve("123456789012345678901234:0")("test.jpeg")
        there was one(urlSigner).signUrl(any[String], beCloseInTimeTo(expectedDate))
      }

    }
  }
}
