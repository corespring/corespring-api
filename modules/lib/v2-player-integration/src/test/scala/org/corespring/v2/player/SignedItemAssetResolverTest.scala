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
      val invalidDomain = Some("invalid-domain")
      val emptyDomain = None
      val emptyVersion = None
      val version = Some("1234")

      val urlSigner = {
        val m = mock[CdnUrlSigner]
        m.signUrl(any[String], any[Date]) returns "fake signed url"
        m
      }

      def createAndResolve(domain: Option[String], validInHours: Int, version: Option[String]):String = {
        val sut = new SignedItemAssetResolver(domain, validInHours, urlSigner, version)
        sut.resolve("123456789012345678901234:0")("test.jpeg")
      }

    }

    def beCloseInTimeTo(date: Date, timeDiff: Int = 500) = new Matcher[Date] {
      def apply[D <: Date](e: Expectable[D]) =
        result((e.value.getTime - date.getTime) < timeDiff,
          "Dates are nearly at the same time",
          "Dates are different",
          e)
    }

    def catchAndReturn(block:(Any=>Any)): Option[Throwable] = {
      try {
        block()
        None
      } catch {
        case t: Throwable => Some(t)
      }
    }


    "resolve" should {

      "throw error when domain is not set" in new scope {
        catchAndReturn { _ =>
          createAndResolve(emptyDomain, 24, version)
        } equals Some(new IllegalArgumentException("domain is not defined"))
      }

      "throw error when domain is not valid" in new scope {
        catchAndReturn { _ =>
          createAndResolve(invalidDomain, 24, version)
        } equals Some( new IllegalArgumentException("domain must start with two slashes. Actual domain is: invalid-domain"))
      }

      "throw error when validInHours is <= 0" in new scope {
        catchAndReturn { _ =>
          createAndResolve(validDomain, 0, version)
        } equals Some( new IllegalArgumentException("validInHours should be an Int >= 0"))
      }

      "return signed url" in new scope {
        createAndResolve(validDomain, 24, version) === "fake signed url"
      }

      "append version" in new scope {
        createAndResolve(validDomain, 24, version)
        there was one(urlSigner).signUrl(find("version=1234"), any[Date])
      }

      "not append version if it is None" in new scope {
        createAndResolve(validDomain, 24, version = None)
        there was one(urlSigner).signUrl(not(find("version=1234")), any[Date])
      }

      "use domain" in new scope {
        createAndResolve(validDomain, 24, version)
        there was one(urlSigner).signUrl(find("://domain/"), any[Date])
      }

      "use https protocol" in new scope {
        createAndResolve(validDomain, 24, version)
        there was one(urlSigner).signUrl(find("https://"), any[Date])
      }

      "use correct s3 path" in new scope {
        createAndResolve(validDomain, 24, version)
        there was one(urlSigner).signUrl(find("/123456789012345678901234/0/data/test.jpeg"), any[Date])
      }

      "pass correct valid-until-date" in new scope {
        val durationInHours = 24
        val expectedDate = DateTime.now().plusHours(durationInHours).toDate
        createAndResolve(validDomain, durationInHours, version)
        there was one(urlSigner).signUrl(any[String], beCloseInTimeTo(expectedDate))
      }

    }
  }
}
