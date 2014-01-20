package org.corespring.v2player.integration

import org.corespring.common.encryption.AESCrypto
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.scopes.data
import org.specs2.execute.{Result => SpecsResult}
import play.api.Logger
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import scala.concurrent.Future
import org.slf4j.LoggerFactory

class LoadPlayerJsThenLoadPlayerTest extends IntegrationSpecification {

  override def logger = LoggerFactory.getLogger("it.v2player")

  val playerLauncher = org.corespring.container.client.controllers.routes.PlayerLauncher
  val playerHooks = org.corespring.container.client.controllers.hooks.routes.PlayerHooks

  val js = playerLauncher.playerJs()


  "when I load the player js with orgId and options" should {

    "fail if i don't pass in the session" in
      loadJsThenCreateSession(
        SEE_OTHER,
        addCookies = false,
        expectedLocation = Some("/login"))

    "allow me to create a session" in loadJsThenCreateSession()
    "allow me to create a session and load player" in loadJsThenCreateSessionThenLoadPlayer
  }

  /**
   * Wrap data up with the vals we need, otherwise one gets AST runtime compiler errors
   */
  class loader(val expectedStatus:Int,val addCookies:Boolean,val expectedLocation:Option[String]) extends data

  def getResultFor[T](request: Request[T])(implicit writable: Writeable[T]): Option[Future[SimpleResult]] = {
    val result: Option[Future[SimpleResult]] = route(request)
    result.filter {
      r => {
        val s = status(r)
        def okStatus = s == SEE_OTHER || s == OK

        if (!okStatus) {
          logger.warn(s"${request.path} status: $s")
          logger.warn(s"${request.path} content: ${contentAsString(r)}")
        }
        okStatus
      }
    }
  }

  def createSessionRequest(call: Call, c: Cookies): Request[AnyContentAsEmpty.type] = {
    val req = FakeRequest(call.method, call.url)
    req.withCookies(c.toSeq: _*)
  }

  def getResultAndCookiesForCreateSession(url:String, addCookies:Boolean, createSession:Call) = {
    for {
      jsResult <- getResultFor(FakeRequest(GET, url))
      cookies <- if(addCookies) Some(cookies(jsResult)) else Some(Cookies(None))
      createSessionResult <- getResultFor(createSessionRequest(createSession,cookies))
    } yield (createSessionResult,cookies)
  }

  def getEncryptedOptions(apiClient:ApiClient, options : PlayerOptions = PlayerOptions.ANYTHING) = {
    val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
    val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
    s"${js.url}?apiClient=${apiClient.clientId}&options=$encrypted"
  }


  def loadJsThenCreateSession(
                               expectedStatus: Int = SEE_OTHER,
                               addCookies: Boolean = true,
                               expectedLocation : Option[String] = None): SpecsResult = new loader(expectedStatus, addCookies, expectedLocation) {

    val createSession = playerHooks.createSessionForItem(itemId.toString)

    val url = getEncryptedOptions(apiClient)
    val resultAndCookies = getResultAndCookiesForCreateSession(url, addCookies, createSession)

    resultAndCookies.map(_._1) match {
      case Some(r) => {
        Logger.debug(contentAsString(r))
        status(r) === expectedStatus
        if(expectedStatus == SEE_OTHER && expectedLocation.isDefined){
          headers(r).get("Location").get === expectedLocation.get
        }
      }
      case _ => failure
    }
  }

  def loadJsThenCreateSessionThenLoadPlayer: SpecsResult = new data {
    val createSession = playerHooks.createSessionForItem(itemId.toString)

    def loadPlayer(result: Future[SimpleResult], cookies: Cookies) : Option[FakeRequest[AnyContentAsEmpty.type]] = {
      header("Location", result).map( l => {
        logger.debug(s"loadPlayer location is: $l")
        FakeRequest(GET,l).withCookies(cookies.toSeq : _*)
      })
    }

    val url = getEncryptedOptions(apiClient)

    val out = for {
      result <- getResultAndCookiesForCreateSession(url,true,createSession)
      playerRequest <- loadPlayer(result._1, result._2)
      loadPlayerResult <- getResultFor(playerRequest)
    } yield loadPlayerResult

    out match {
      case Some(result) => success
      case _ => failure
    }
  }


}
