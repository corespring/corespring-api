package org.corespring.lti.web.controllers

import com.mongodb.casbah.commons.MongoDBObject
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import org.bson.types.ObjectId
import org.corespring.lti.models.{LtiQuestion, LtiQuiz, LtiOAuthConsumer, LtiData}
import org.corespring.lti.web.controllers
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.itemSession.ItemSessionSettings
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{WithApplication, FakeHeaders}


class AssignmentLauncherTest extends Specification /*extends BaseTest*/ {

  case class FakeRequestWithHost[A](
                                     method: String,
                                     uri: String,
                                     headers: FakeHeaders,
                                     body: A,
                                     remoteAddress: String = "127.0.0.1",
                                     hostOverride: String = "http://localhost") extends play.api.mvc.Request[A] {
    def id: Long = 1

    def tags: Map[String, String] = Map()

    def path: String = "path"

    def version: String = "version"

    def queryString: Map[String, Seq[String]] = Map()
  }


  val routes = org.corespring.lti.web.controllers.routes.AssignmentLauncher


  val MockOrgId: ObjectId = new ObjectId("51114b307fc1eaa866444648")

  def getOrg: Organization = Organization.findOneById(MockOrgId).get

  val Call = org.corespring.lti.web.controllers.routes.AssignmentLauncher.launch()

  /**
   * Execute a POST Form request w/ oauth_signature for the given org.
   * @param params - additional params to send
   * @return
   */
  def callWithApiClient(apiClient: ApiClient, params: (String, String)*): Result = {

    val defaultParams: Map[String, String] = Map(
      AssignmentLauncher.LtiKeys.ConsumerKey -> apiClient.clientId.toString,
      LtiData.Keys.OutcomeServiceUrl -> "service_url",
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_callback" -> "about%3Ablank",
      "oauth_timestamp" -> "1355143263",
      "oauth_nonce" -> "NNRHA0eRjU0mhTxjByFrINfn4Z1dmBmVIuJiFg"
    )

    val allParams: Map[String, String] = defaultParams ++ params

    def asForm(m: Map[String, String]): Map[String, Seq[String]] = m.map((kv) => (kv._1, Seq(kv._2)))

    def makeFake(c: Call, form: Map[String, Seq[String]]): Request[AnyContentAsFormUrlEncoded] = {
      new FakeRequestWithHost(c.method, c.url, new FakeHeaders(), AnyContentAsFormUrlEncoded(form), hostOverride = "localhost:9000")
    }

    def getSignature(key: String, secret: String, request: Request[AnyContent]): String = {
      val consumer: LtiOAuthConsumer = new LtiOAuthConsumer(key, secret)
      consumer.sign(request)
      consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
      consumer.getOAuthSignature().get
    }

    val request = makeFake(Call, asForm(allParams))
    val signature = getSignature(apiClient.clientId.toString, apiClient.clientSecret, request)
    val finalParams = allParams + ("oauth_signature" -> signature)
    val finalRequest: Request[AnyContentAsFormUrlEncoded] = makeFake(Call, asForm(finalParams))

    controllers.AssignmentLauncher.launch()(finalRequest)
  }

  def configureLaunchConfig(resourceLinkId: String, itemId: VersionedId[ObjectId], client: ApiClient): LtiQuiz = {
    LtiQuiz.findByResourceLinkId(resourceLinkId) match {
      case Some(config) => {
        val newConfig = config.copy(question = LtiQuestion(itemId = Some(itemId), ItemSessionSettings()))
        LtiQuiz.update(newConfig, client.orgId)
        newConfig
      }
      case _ => {

        val newConfig = LtiQuiz(
          resourceLinkId = resourceLinkId,
          question = LtiQuestion(itemId = Some(itemId), ItemSessionSettings()),
          participants = Seq(),
          orgId = Some(client.orgId))
        LtiQuiz.insert(newConfig)
        newConfig
      }
    }
  }

  "Assignment launcher" should {

    "launching as an instructor creates a new launch config" in new WithApplication {

      val org = getOrg
      val apiClient: ApiClient = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> org.id)).get

      val uid = "launch_as_instructor"

      val result = callWithApiClient(apiClient,
        (LtiData.Keys.Roles, "Instructor"),
        (LtiData.Keys.ResourceLinkId, uid))

      LtiQuiz.findByResourceLinkId(uid).map {
        config =>
          config.orgId === Some(apiClient.orgId)
          config.question.itemId === None
          status(result) === OK
      }.getOrElse(failure("???"))
    }

    "launching as a student returns a redirect to the player if the teacher has configured an item id" in new WithApplication {
      val org = getOrg
      val apiClient = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> org.id)).get
      val config = configureLaunchConfig("1", VersionedId(new ObjectId(), Some(0)), apiClient)
      val expectedRedirectCall = org.corespring.lti.web.controllers.routes.AssignmentPlayer.run(config.id, "1")

      val result = callWithApiClient(apiClient,
        (LtiData.Keys.Roles -> "Student"),
        (LtiData.Keys.ResourceLinkId -> "1"),
        (LtiData.Keys.ResultSourcedId -> "1"),
        (LtiData.Keys.LaunchPresentationReturnUrl -> "some_return_url")
      )
      status(result) === SEE_OTHER
      header("Location", result).getOrElse("?") === expectedRedirectCall.url
    }
  }

}
