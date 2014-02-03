package org.corespring.v2player.integration

import org.corespring.it.{ IntegrationHelpers, IntegrationSpecification }
import org.corespring.v2player.integration.scopes.data
import org.slf4j.LoggerFactory
import org.specs2.execute.{ Result => SpecsResult }
import play.api.Logger
import play.api.mvc._
import play.api.test.FakeRequest
import scala.concurrent.Future

class LoadPlayerJsThenLoadPlayerTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.v2player")

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
  class loader(val expectedStatus: Int, val addCookies: Boolean, val expectedLocation: Option[String]) extends data

  def createSessionRequest(call: Call, c: Cookies): Request[AnyContentAsEmpty.type] = {
    val req = FakeRequest(call.method, call.url)
    req.withCookies(c.toSeq: _*)
  }

  def getResultAndCookiesForCreateSession(url: String, addCookies: Boolean, createSession: Call) = {
    for {
      jsResult <- getResultFor(FakeRequest(GET, url))
      cookies <- if (addCookies) Some(cookies(jsResult)) else Some(Cookies(None))
      createSessionResult <- getResultFor(createSessionRequest(createSession, cookies))
    } yield (createSessionResult, cookies)
  }

  def loadJsThenCreateSession(
    expectedStatus: Int = SEE_OTHER,
    addCookies: Boolean = true,
    expectedLocation: Option[String] = None): SpecsResult = new loader(expectedStatus, addCookies, expectedLocation) {

    val createSession = playerHooks.createSessionForItem(itemId.toString)

    val url = getEncryptedOptions(js, apiClient)
    val resultAndCookies = getResultAndCookiesForCreateSession(url, addCookies, createSession)

    resultAndCookies.map(_._1) match {
      case Some(r) => {
        Logger.debug(contentAsString(r))
        status(r) === expectedStatus
        if (expectedStatus == SEE_OTHER && expectedLocation.isDefined) {
          headers(r).get("Location").get === expectedLocation.get
        }
      }
      case _ => failure
    }
  }

  def loadJsThenCreateSessionThenLoadPlayer: SpecsResult = new data {
    val createSession = playerHooks.createSessionForItem(itemId.toString)

    def loadPlayer(result: Future[SimpleResult], cookies: Cookies): Option[FakeRequest[AnyContentAsEmpty.type]] = {
      header("Location", result).map(l => {
        logger.debug(s"loadPlayer location is: $l")
        FakeRequest(GET, l).withCookies(cookies.toSeq: _*)
      })
    }

    val url = getEncryptedOptions(js, apiClient)

    val out = for {
      result <- getResultAndCookiesForCreateSession(url, true, createSession)
      playerRequest <- loadPlayer(result._1, result._2)
      loadPlayerResult <- getResultFor(playerRequest)
    } yield loadPlayerResult

    out match {
      case Some(result) => success
      case _ => failure
    }
  }

}
