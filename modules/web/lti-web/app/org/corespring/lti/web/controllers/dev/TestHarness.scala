package org.corespring.lti.web.controllers.dev

import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import org.bson.types.ObjectId
import org.corespring.common.url.BaseUrl
import org.corespring.lti.models.{LtiOAuthConsumer, LtiData}
import org.corespring.lti.web.controllers.AssignmentLauncher
import org.corespring.lti.web.controllers.dev.routes.{TestHarness=>TestHarnessRoutes}
import org.corespring.lti.web.controllers.routes.{AssignmentLauncher => AssignmentLauncherRoutes}
import org.corespring.lti.web.views.html.dev._
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.auth.ApiClient
import org.jboss.netty.handler.codec.http.HttpMethod
import play.Logger
import play.api.mvc._
import scala.Some
import securesocial.core.SecureSocial
import securesocial.core.SecuredRequest

/**
 * This test harness simulates an LTI 1.1 Tool Consumer
 * @see http://www.imsglobal.org/LTI/v1p1p1/ltiIMGv1p1p1.html
 */
object TestHarness extends BaseApi with SecureSocial {

  private var passbackText: String = ""
  /**
   * The initial form where you can set up your settings
   * @return
   */
  def begin = SecuredAction(false) {
    request: SecuredRequest[AnyContent] =>
      {
        val url = TestHarnessRoutes.prepare().url
        Ok(org.corespring.lti.web.views.html.dev.begin(url))
      }
  }

  /**
   * Takes the form from launch and renders out a new form with all the parameters required by lti.
   * This form the does an autosubmit - same as LTI.
   * @return
   */
  def prepare = ApiAction {
    request =>

      val uri = request.uri
      val host = request.host
      val orgId = request.ctx.organization
      val referrer = request.headers.get("Referer")

      val root = BaseUrl(request)
      Logger.info("root: " + root)

      request.body.asFormUrlEncoded match {
        case Some(formParams) => {

          val url = AssignmentLauncherRoutes.launch().url
          val trimmed = formParams.filter {
            kv =>
              print(kv)
              !kv._2(0).isEmpty
          }
          val out = trimmed.map((kv) => (kv._1, kv._2.head))

          val client: ApiClient = getOrCreate(orgId)
          val orgParams = Map(
            AssignmentLauncher.LtiKeys.ConsumerKey -> client.clientId.toString,
            "oauth_signature_method" -> "HMAC-SHA1",
            "oauth_callback" -> "about%3Ablank",
            "oauth_timestamp" -> "1355143263",
            "oauth_nonce" -> "NNRHA0eRjU0mhTxjByFrINfn4Z1dmBmVIuJiFg",
            LtiData.Keys.OutcomeServiceUrl -> (root + TestHarnessRoutes.gradePassback().url),

            LtiData.Keys.LaunchPresentationReturnUrl -> (root + TestHarnessRoutes.begin().url))
          val allParams = out ++ orgParams
          println(allParams)
          def asForm(m: Map[String, String]): Map[String, Seq[String]] = m.map((kv) => (kv._1, Seq(kv._2)))
          val form = asForm(allParams)

          val protocol = if (root.startsWith("https")) "https" else "http"

          val mockHeaders = new Headers {
            override def keys: Set[String] = Set("x-forward-proto=" + protocol)

            protected val data: Seq[(String, Seq[String])] = Seq()

            override def getAll(key: String): Seq[String] = Seq(protocol)
          }

          val request = new SimplePostRequest[AnyContentAsFormUrlEncoded](
            url,
            url,
            AnyContentAsFormUrlEncoded(form),
            mockHeaders,
            host)

          val signature = getSignature(client.clientId.toString, client.clientSecret, request)

          //TODO: 2.1.2 Upgrade - UserKey? what to use now?
          Ok(autoSubmitForm(url, allParams + ("oauth_signature" -> signature)))
            .withSession(request.session - "SecureSocial.UserKey")
        }
        case _ => Ok("Couldn't prepare form")
      }
  }

  def inspectGradePassback = Action {
    request =>
      println("passbackText: " + passbackText)
      try {
        val xml = scala.xml.XML.loadString(passbackText)
        val score = (xml \\ "resultScore" \ "textString").text.trim
        Ok( grade(score))
      } catch {
        case e: Throwable => Ok("An error occured parsing: " + passbackText + ", " + e.getMessage)
      }
  }

  def clearGradePassback = Action {
    request =>
      passbackText = ""
      Ok("")
  }

  def gradePassback = Action(parse.tolerantText) {
    request =>
      println("Grade passback received:")
      passbackText = request.body.toString
      println(passbackText)
      Ok("")
  }

  /**
   * Create a client if none is found
   * @param orgId
   * @return
   */
  def getOrCreate(orgId: ObjectId): ApiClient = {
    ApiClient.findOneByOrgId(orgId) match {
      case Some(c) => c
      case _ => {
        val c = new ApiClient(orgId, new ObjectId(), new ObjectId().toString)
        ApiClient.insert(c)
        c
      }
    }
  }

  case class SimplePostRequest[A](uri: String, path: String, body: A, headers: Headers, hostOverride: String = "localhost:9000") extends Request[A] {

    def queryString: Map[String, Seq[String]] = Map()

    def remoteAddress: String = ""

    def method: String = HttpMethod.POST.toString

    override lazy val host: String = hostOverride

    override def version: String = "mock"

    override def tags: Map[String, String] = Map()

    override def id: Long = 1
  }

  private def getSignature(key: String, secret: String, request: Request[AnyContent]): String = {
    Logger.info("TestHarness:getSignature")
    val consumer: LtiOAuthConsumer = new LtiOAuthConsumer(key, secret)
    consumer.sign(request)
    consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
    consumer.getOAuthSignature().get
  }
}
