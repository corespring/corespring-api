package basiclti.controllers.dev

import play.api.mvc._
import securesocial.core.SecureSocial
import models.auth.ApiClient
import controllers.auth.{OAuthConstants, BaseApi}
import basiclti.controllers.AssignmentLauncher
import basiclti.models.{LtiOAuthConsumer, LtiData}
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import scala.Some
import org.jboss.netty.handler.codec.http.HttpMethod
import org.bson.types.ObjectId
import play.Logger
import play.api.Play

object TestHarness extends BaseApi with SecureSocial {

  /**
   * The initial form where you can set up your settings
   * @return
   */
  def begin = {
    val url = basiclti.controllers.dev.routes.TestHarness.prepare().url

    if(Play.isDev(Play.current)){
      Action{ request =>
        Ok(basiclti.views.html.dev.launchItemChooser(url))
          .withSession(OAuthConstants.AccessToken -> common.mock.MockToken)
      }
    }
    else {
      SecuredAction() {
        request =>
          Ok(basiclti.views.html.dev.launchItemChooser(url))
    }
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

      Logger.info("uri: " + uri)
      Logger.info("host: " + host)

      request.body.asFormUrlEncoded match {
        case Some(formParams) => {
          val url = basiclti.controllers.routes.AssignmentLauncher.launch().url
          val out = formParams.map((kv) => (kv._1, kv._2.head))

          val client: ApiClient = getOrCreate(orgId)
          val orgParams = Map(
            AssignmentLauncher.LtiKeys.ConsumerKey -> client.clientId.toString,
            "oauth_signature_method" -> "HMAC-SHA1",
            "oauth_callback" -> "about%3Ablank",
            "oauth_timestamp" -> "1355143263",
            "oauth_nonce" -> "NNRHA0eRjU0mhTxjByFrINfn4Z1dmBmVIuJiFg"
          )
          val allParams = out ++ orgParams
          println(allParams)
          def asForm(m:Map[String,String]) : Map[String,Seq[String]] = m.map( (kv) => (kv._1, Seq(kv._2)))
          val form = asForm(allParams)
          val request = new SimplePostRequest[AnyContentAsFormUrlEncoded](
            url,
            url,
            AnyContentAsFormUrlEncoded(form),
            host)
          val signature = getSignature(client.clientId.toString, client.clientSecret, request)

          Ok(basiclti.views.html.dev.autoSubmitForm(url, allParams + ("oauth_signature" -> signature)))
        }
        case _ => Ok("Couldn't prepare form")
      }
  }

  /**
   * Create a client if none is found
   * @param orgId
   * @return
   */
  def getOrCreate(orgId:ObjectId) : ApiClient = {
    ApiClient.findByOrgId(Some(orgId)) match {
      case Some(c) => c
      case _ => {
        val c = new ApiClient(orgId, new ObjectId(), new ObjectId().toString)
        ApiClient.insert(c)
        c
      }
    }
  }


  case class SimplePostRequest[A](uri:String, path:String, body:A, hostOverride : String = "localhost:9000") extends Request[A] {

    def queryString: Map[String, Seq[String]] = Map()
    def remoteAddress : String = ""
    def method : String = HttpMethod.POST.toString
    override lazy val host : String = hostOverride
    def headers : Headers = new Headers {
      def keys: Set[String] = Set()
      def getAll(key: String): Seq[String] = Seq()
    }
  }

  private def getSignature(key:String, secret : String, request:Request[AnyContent]) : String = {
    val consumer : LtiOAuthConsumer = new LtiOAuthConsumer(key, secret)
    consumer.sign(request)
    consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
    consumer.getOAuthSignature().get
  }
}
