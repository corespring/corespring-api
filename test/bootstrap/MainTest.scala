package bootstrap

import java.net.URL

import com.mongodb.casbah.{ MongoConnection, MongoURI }
import filters.CacheFilter
import org.bson.types.ObjectId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.FakeRequest
import play.api.{ Configuration, Mode }

class MainTest extends Specification with Mockito {

  /**
   * Note:
   * should really be mocking MongoDB, but that would
   * take alot of effort, so for now just giving it a db uri
   * that isn't used anywhere.
   */

  val uri = "mongodb://localhost/cs-api-main-test-mock"
  val archiveUri = "mongodb://localhost/cs-api-archive-test-mock"

  val db = {
    val u = MongoURI(uri)
    val conn = MongoConnection(u)
    conn("cs-api-main-test-mock")
  }

  val archiveDb = MongoConnection(MongoURI(archiveUri))("cs-api-archive-test-mock")

  def mkConfig(domain: String, queryParam: Boolean) = Map(
    "DEMO_ORG_ID" -> ObjectId.get.toString,
    "ROOT_ORG_ID" -> ObjectId.get.toString,
    "COMPONENT_FILTERING_ENABLED" -> false,
    "mongodb.default.uri" -> uri,
    "archive" -> Map(
      "contentCollectionId" -> ObjectId.get.toString,
      "orgId" -> ObjectId.get.toString),
    "container.editor.autosave.debounceInMillis" -> 1000,
    "ELASTIC_SEARCH_URL" -> "http://elastic-search.com",
    "container.editor.autosave.debounceInMillis" -> 500,
    "container.upload.audio.maxSizeKb" -> 400,
    "container.upload.image.maxSizeKb" -> 300,
    "container.common.DEV_TOOLS_ENABLED" -> false,
    "container.cdn.domain" -> domain,
    "container.components.path" -> "path",
    "container.cdn.add-version-as-query-param" -> queryParam)

  def resourceAsUrl(s: String): Option[URL] = None

  val config = mkConfig("//blah.com", false)

  "Main" should {
    "use new CacheFilter" in {
      val main = new Main(
        db,
        archiveDb,
        Configuration.from(config),
        Mode.Test,
        this.getClass.getClassLoader,
        resourceAsUrl _)
      main.componentSetFilter must haveInterface[CacheFilter]
    }

    "should use play mode from config" in {
      val main = new Main(db,
        archiveDb,
        Configuration.from(config + ("APP_MODE_OVERRIDE" -> "Prod")),
        Mode.Test,
        this.getClass.getClassLoader,
        resourceAsUrl _)
      main.playMode must_== Mode.Prod
    }

    "should use play mode from arguments, when mode is not set in config" in {
      val main = new Main(db,
        archiveDb,
        Configuration.from(config),
        Mode.Test,
        this.getClass.getClassLoader,
        resourceAsUrl _)
      main.playMode must_== Mode.Test
    }
  }

  "resolveDomain" should {

    "return the path directly if no cdn is configured" in {
      val minusCdn = config - "container.cdn.domain"
      val main = new Main(db, archiveDb, Configuration.from(minusCdn), Mode.Test, this.getClass.getClassLoader, resourceAsUrl _)
      main.resolveDomain("hi") must_== "hi"
    }

    "return the path with the cdn prefixed if the cdn is configured" in {
      val main = new Main(db, archiveDb, Configuration.from(config), Mode.Test, this.getClass.getClassLoader, resourceAsUrl _)
      main.resolveDomain("hi") must_== "//blah.com/hi"
    }
  }

  "itemAssetResolver.resolve" should {
    trait scope extends Scope {
      val itemId = "123456789012345678901234:0"
      val file = "test.jpeg"
      def mkItemAssetResolverConfig(enabled: Boolean, signUrls: Boolean) = {
        val mainConfig = mkConfig("//blah", false)
        val iarConfig = Map(
          "item-asset-resolver.enabled" -> enabled,
          "item-asset-resolver.add-version-as-query-param" -> false,
          "item-asset-resolver.sign-urls" -> signUrls,
          "item-asset-resolver.domain" -> "//blah",
          "item-asset-resolver.key-pair-id" -> "APKAI3FT54PDZMY3U24A",
          "item-asset-resolver.private-key" -> "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA2ivBexp6pxfWupSR06rdwJHkXpd48hqw8XSzV/AWtkel6bnw\nBl1ltE5YrOoyOA0VjFw6jNj+0XmSgt1cUjBbQ8vM5dzTDUHeePQQtsNVmF7WErQT\n9d2LHoAX42HgwKbmXjXnYiTyMqO87q2jyhvt7q9wzk6gSI0gtvnm0iYf0DXYK57G\nScmoGnxDnDKkW0HP3eJrWakAcSScapK4/Jv5cnDZ8nQIS9Z5XxaXzE3djIkO46p/\nzu4BCXFDRwqnaWn5yiNP9c4WNQLB38VBej3hgfRQYrMzTqn6M8K3PCYUj3nUb0xB\nUv0+lQ+p1b8Oe5cBySjhslloQ0tg3JEntvaJuQIDAQABAoIBADXcBXjRkaP8g5su\nIE4D6Zinq5wagtYp9rK1H60+7Sx0xaXMrE+18OyxRrzxWBJ0UHSFNEMfMtEd1SiP\nY0I7A9zZzCyW9ldYgoaToiisUk46Y1jcsezJk5WlA8CzohuNWGO7pPKaslwEBhla\nLowvlu6MyylzSah/hqsFSJFqrgHlRFAhvQ0udSrx8/6ae5uBUqoq+bkSb1hX0ive\nGm9Xko9r3+LWj83xVnGKIb0tZFofO5C/qnRgHa6KiOwNrMZa/HgRc5OTTxEMo+Bm\novfbpj0EDPtfdUDF0bKipVrAU1iw0c6Q6kgKFoLKUKnxDu9A+VN2tsIbCXakekGj\nPYjUiAECgYEA8cDDidVoKjwjGXXXXDNSJoiuCzE65SnV9A6clrtcOMyfpPLiSy8H\n1rbir8DIFw03uUypbvUyqkenBq598lRo/Rhw2CRK4XLalTcj9E2hy+Fc5lcZeXd4\nJ0gV6LmqjsMh17vXacn+S8YDq6y52GZGQB/qUDyX++2b7z8Sxl7sLQECgYEA5wc3\n4HYIwDlH80BIauM74MjcRmW50BtpY+4E4EQY2Gn3w0Rsf0Ck20oYKhHFQ75xMOF+\nvpodSECz4ye03mlD14p5JaIn+fhrJ3/w6rdBt8lYzYB6Vs0I/kCnofpAcCbxhoNX\n10BEg27IV6aktXgZinq9cvzfCOh962MVTA+WBLkCgYAgSrFTze+2BIZjtjvoEurc\nPtGQqSjGx4nOqcz8zVYKODry24aiqEuRwKgS9dtESP2ygKz5J0N3P07uM4ybO+8y\nL3uTQ3XFG4Ra/hyNW3lLNHUmR2gds3mXNafHiFVh8Gqq2GpztQmEsZR38AB7CV5E\n3n577TwX6Ks1j+VAHhnKAQKBgQDFDmce0g8Mxs8UMRST91avmSQp98LSO09dqTwH\nfo4iqeBncgmJUT5MvZp258l2yw4JP424TgQECQxnCQtBWlA/nSFQdEvc74OWoY6A\n5ebsOJXCU4AGYcT1+XgCtU4ZW15P+eAG/g5yfR/tg3qiPtqqP58wYXhsRMKC8HTN\n981iIQKBgQC8Lf16Auex9vIrkX9dlR7RLo6uiUedjRLU2s3y2EX7no1v+pC8VW8W\n0cGqQZZMjTGAdTT1sR9r/+D24NOHKYV1lzDbit+G0MJK/ET27BdPP8v+i/wcad3M\nVM5uR8Qn7Oa3VycHrDNPL22912awnmbRmwP+AIvTkN5g9VLfqbGPOA==\n-----END RSA PRIVATE KEY-----")
        mainConfig ++ iarConfig
      }

      def mkMain(config: Map[String, Any]) = {
        new Main(db, archiveDb, Configuration.from(config), Mode.Test, this.getClass.getClassLoader, resourceAsUrl _)
      }
    }
    "return the file when enabled is false" in new scope {
      val main = mkMain(mkItemAssetResolverConfig(false, true))
      main.itemAssetResolver.resolve(itemId)(file) === "test.jpeg"
    }

    "return the unsigned url when signUrl is false" in new scope {
      val main = mkMain(mkItemAssetResolverConfig(true, false))
      main.itemAssetResolver.resolve(itemId)(file) === "//blah/player/item/123456789012345678901234:0/test.jpeg"
    }

    "return the signed url when signUrl is true" in new scope {
      val main = mkMain(mkItemAssetResolverConfig(true, true))
      main.itemAssetResolver.resolve(itemId)(file) must startingWith("https://blah/player/item/123456789012345678901234:0/test.jpeg?Expires=")
    }

    "encode the filename" in new scope {
      val main = mkMain(mkItemAssetResolverConfig(true, true))
      val fileWithBlank = "a b c.png"
      main.itemAssetResolver.resolve(itemId)(fileWithBlank) must startingWith("https://blah/player/item/123456789012345678901234:0/a%20b%20c.png?Expires=")
    }
  }

}
