package tests.basiclti.controllers

import org.specs2.mutable.Specification
import tests.PlaySingleton
import play.api.test.{FakeHeaders, FakeRequest}
import basiclti.controllers.AssignmentLauncher
import basiclti.models.{LtiLaunchConfiguration, LtiRequestAdapter, LtiOAuthConsumer, LtiData}
import play.api.test.Helpers._
import play.api.mvc._
import models.{ContentCollection, Organization}
import models.auth.ApiClient
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import play.api.test.FakeHeaders
import scala.Some
import play.api.mvc.AnyContentAsFormUrlEncoded

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

  /**
   * Execute a POST Form request w/ oauth_signature for the given org.
   * @param params - additional params to send
   * @return
   */
  def callWithApiClient( apiClient : ApiClient, params : (String,String)* ) : Option[Result] = {

    val defaultParams : Map[String, String] = Map(
      AssignmentLauncher.LtiKeys.ConsumerKey -> apiClient.clientId.toString,
      LtiData.Keys.OutcomeServiceUrl -> "service_url",
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_callback" -> "about%3Ablank",
      "oauth_timestamp" -> "1355143263",
      "oauth_nonce" -> "NNRHA0eRjU0mhTxjByFrINfn4Z1dmBmVIuJiFg"
    )

    val allParams : Map[String,String] = defaultParams ++ params

    def asForm(m:Map[String,String]) : Map[String,Seq[String]] = m.map( (kv) => (kv._1, Seq(kv._2)))

    def makeFake(c:Call, form:Map[String,Seq[String]]) : FakeRequest[AnyContentAsFormUrlEncoded] = {
        new FakeRequestWithHost(c.method, c.url, new FakeHeaders(), AnyContentAsFormUrlEncoded(form), hostOverride = "localhost:9000" )
    }

    def getSignature(key:String, secret : String, request:Request[AnyContent]) : String = {
      val consumer : LtiOAuthConsumer = new LtiOAuthConsumer(key, secret)
      consumer.sign(request)
      consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
      consumer.getOAuthSignature().get
    }

    val request = makeFake(Call,asForm(allParams))
    val signature = getSignature(apiClient.clientId.toString, apiClient.clientSecret, request)
    val finalParams = allParams + ("oauth_signature" -> signature )
    val finalRequest = makeFake(Call, asForm(finalParams))
    routeAndCall(finalRequest)
  }

  def getOrg : Organization = Organization.findOneById(MockOrgId).get

  def configureLaunchConfig(resourceLinkId:String, itemId:ObjectId, client : ApiClient ) : LtiLaunchConfiguration = {
    LtiLaunchConfiguration.findByResourceLinkId(resourceLinkId) match {
      case Some(config) => {
        val newConfig = config.copy(itemId = Some(itemId))
        LtiLaunchConfiguration.update(newConfig, client.orgId)
        newConfig
      }
      case _ => {
        val newConfig = new LtiLaunchConfiguration(
          resourceLinkId = resourceLinkId,
          itemId = Some(itemId),
          orgId = Some(client.orgId),
          sessionSettings = None)
        LtiLaunchConfiguration.insert(newConfig)
        newConfig
      }
    }
  }

  "Assignment launcher" should {

    "launching as an instructor creates a new launch config" in {

      val org = getOrg
      val apiClient : ApiClient = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> org.id)).get

      val uid = "launch_as_instructor"

      val result = callWithApiClient(apiClient,
        (LtiData.Keys.Roles, "Instructor"),
        (LtiData.Keys.ResourceLinkId, uid) )

      result match {
        case Some(r) => {
          LtiLaunchConfiguration.findByResourceLinkId(uid) match {
            case Some(config) => {
              config.orgId === Some(apiClient.orgId)
              config.itemId === None
            }
            case _ => failure("no launch config found")
          }
          status(r) === OK
        }
        case _ => failure("no result")
      }
    }


    "launching as a student returns a redirect to the player if the teacher has configured an item id" in {
      val org = getOrg
      val apiClient = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> org.id)).get
      val config = configureLaunchConfig("1", new ObjectId(), apiClient)
      val expectedRedirectCall = basiclti.controllers.routes.AssignmentPlayer.run(config.id, "1")

      val result = callWithApiClient(apiClient,
        (LtiData.Keys.Roles -> "Student"),
        (LtiData.Keys.ResourceLinkId -> "1"),
        (LtiData.Keys.ResultSourcedId -> "1"),
        (LtiData.Keys.LaunchPresentationReturnUrl -> "some_return_url")
      )
      result match {
        case Some(r) => {
          status(r) === SEE_OTHER
          header("Location", r).getOrElse("?") === expectedRedirectCall.url
        }
        case _ => failure("no result returned")
      }
    }

    def getTestApiClient = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> getOrg.id)).get

    "launching with 'select_link' shows the chooser and doesn't store a link id" in {

      val client = getTestApiClient

      val linkId = "some_id_that_should_be_ignored"
      val result = callWithApiClient(client,
        (LtiData.Keys.SelectionDirective -> "select_link"),
        (LtiData.Keys.ResourceLinkId -> linkId)
      )


      result match {
        case Some(r) => {
          status(r) === OK
          LtiLaunchConfiguration.findByResourceLinkId(linkId) === None
        }
        case _ => failure("should get OK")
      }
    }
  }

}
