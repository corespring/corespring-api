package tests.basiclti.controllers

import org.specs2.mutable.Specification
import tests.PlaySingleton
import play.api.test.{FakeHeaders, FakeRequest}
import basiclti.controllers.AssignmentLauncher
import basiclti.models.LtiData
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContent}
import models.{ContentCollection, Organization}
import play.api.mvc.Result
import models.auth.ApiClient
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId

class AssignmentLauncherTest extends Specification {

  case class FakeRequestWithHost[A](
                                     override val method: String,
                                     override val uri: String,
                                     override val headers: FakeHeaders,
                                     override val body: A,
                                     override val remoteAddress: String = "127.0.0.1",
                                     hostOverride : String = "http://localhost") extends FakeRequest[A](method,uri,headers,body,remoteAddress) with play.api.mvc.Request[A]{
    override lazy val host = hostOverride

  }



  val MockOrgId : ObjectId = new ObjectId( "502404dd0364dc35bb393397" )

  PlaySingleton.start()

  val Call = basiclti.controllers.routes.AssignmentLauncher.launch()

  def callWithOrg( org : Organization, params : (String,String)* ) : Option[Result] = {

    val apiClient : ApiClient = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> org.id)).get

    val defaultParams : Seq[(String, String)] = Seq(
      (AssignmentLauncher.LtiKeys.ConsumerKey, apiClient.clientId.toString),
      (LtiData.Keys.OutcomeServiceUrl, "service_url"),
      ("oauth_signature", "blah"),
      ("oauth_signature_method", "HMAC-SHA1"),
      ("oauth_callback", "about%3Ablank"),
      ("oauth_timestamp", "1355143263"),
      ("oauth_nonce", "NNRHA0eRjU0mhTxjByFrINfn4Z1dmBmVIuJiFg")
    )

    val allParams : Seq[(String,String)] = defaultParams ++ params
    val out: Seq[(String,Seq[String])] = allParams.map( (kv) => (kv._1, Seq(kv._2)))
    val request : FakeRequest[AnyContentAsFormUrlEncoded] =
    new FakeRequestWithHost(Call.method, Call.url, new FakeHeaders(), AnyContentAsFormUrlEncoded(out.toMap), hostOverride = "http://localhost:9000" )
    routeAndCall(request)
  }

  def getOrg : Organization = Organization.findOneById(MockOrgId).get

  "Assignment launcher" should {

    "parse the request info" in {

      val org = getOrg

      val result = callWithOrg(org,
        (LtiData.Keys.Roles, "Instructor"),
        (LtiData.Keys.ResourceLinkId, "1") )

      println("result -->")
      println(contentAsString(result.get))
      result match {
        case Some(r) => status(r) === OK
        case _ => failure
      }

      true === true
    }
  }

}
