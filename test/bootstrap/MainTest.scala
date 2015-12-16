package bootstrap

import java.io.InputStream

import com.mongodb.casbah.{MongoURI, MongoConnection, MongoDB}
import filters.CacheFilter
import org.bson.types.ObjectId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.{Configuration, Mode}

class MainTest extends Specification with Mockito{

  /** Note:
    * should really be mocking MongoDB, but that would
    * take alot of effort, so for now just giving it a db uri
    * that isn't used anywhere.
    */

  val uri = "mongodb://localhost/cs-api-main-test-mock"

  val db = {
    val u = MongoURI(uri)
    val conn = MongoConnection(u)
    conn("cs-api-main-test-mock")
  }

  def mkConfig(domain: String, queryParam: Boolean) = Map(
    "DEMO_ORG_ID" -> ObjectId.get.toString,
    "ROOT_ORG_ID" -> ObjectId.get.toString,
    "COMPONENT_FILTERING_ENABLED" -> false,
    "mongodb.default.uri" -> uri,
    "archive" -> Map(
      "contentCollectionId" -> ObjectId.get.toString,
      "orgId" -> ObjectId.get.toString
    ),
    "ELASTIC_SEARCH_URL" -> "http://elastic-search.com",
    "container" -> Map(
      "components.path" -> "path",
      "cdn" -> Map(
        "domain" -> domain,
        "add-version-as-query-param" -> queryParam)))

  def resourceAsStream(s:String) : Option[InputStream] = None

  val config = mkConfig("//blah.com", false)

    "Main" should {
      "use new CacheFilter" in {
        val main = new Main(db,
          Configuration.from(config),
          Mode.Test,
          this.getClass.getClassLoader,
          resourceAsStream _)
        main.componentSetFilter must haveInterface[CacheFilter]
      }
    }

    "resolveDomain" should {

      "return the path with the cdn prefixed if the cdn is configured" in {
        val main = new Main(db, Configuration.from(config), Mode.Test, this.getClass.getClassLoader, resourceAsStream _)
        main.resolveDomain("hi") must_== "//blah.com/hi"
      }
    }
}
