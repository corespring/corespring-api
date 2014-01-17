package org.corespring.v2player.integration

import org.corespring.common.encryption.AESCrypto
import org.corespring.it.ITSpec
import org.corespring.test.helpers.models.{CollectionHelper, ItemHelper, OrganizationHelper, ApiClientHelper}
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.specs2.mutable.BeforeAfter
import play.api.Logger
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import scala.concurrent.Future

class LoadPlayerJsThenLoadPlayerTest extends ITSpec {

  def logger = Logger("it.v2player")

  val playerLauncher = org.corespring.container.client.controllers.routes.PlayerLauncher
  val playerHooks = org.corespring.container.client.controllers.hooks.routes.PlayerHooks

  val js = playerLauncher.playerJs()

  trait data extends BeforeAfter {
    val orgId = OrganizationHelper.create("org")
    val apiClient = ApiClientHelper.create(orgId)
    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)

    def before: Any = {
      logger.debug(s"data ready: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
    }

    def after: Any = {
      logger.trace(s"deleting db data..${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
      ApiClientHelper.delete(apiClient)
      OrganizationHelper.delete(orgId)
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
    }
  }


  def getResultFor[T](request: Request[T], expectedStatus: Int = OK)(implicit writable: Writeable[T]): Option[Future[SimpleResult]] = {
    val result: Option[Future[SimpleResult]] = route(request)
    result.filter {
      r => {
        val s = status(r)
        if (s != expectedStatus) {
          logger.warn(s"${request.path} status: $s")
          logger.warn(s"${request.path} content: ${contentAsString(r)}")
        }
        s == expectedStatus
      }
    }
  }

  def createSessionRequest(call: Call, c: Cookies): Request[AnyContentAsEmpty.type] = {
    val req = FakeRequest(call.method, call.url)
    req.withCookies(c.toSeq: _*)
  }

  /**
   * Wrap data up with the vals we need
   * @param expectedStatus
   * @param addCookies
   * @param expectedLocation
   */
  class loader(val expectedStatus:Int,val addCookies:Boolean,val expectedLocation:Option[String]) extends data

  import org.specs2.execute.{Result => SpecsResult}


  def loadJsThenCreateSession(
                               expectedStatus: Int = SEE_OTHER,
                               addCookies: Boolean = true,
                               expectedLocation : Option[String] = None): SpecsResult = new loader(expectedStatus, addCookies, expectedLocation) {

    val createSession = playerHooks.createSessionForItem(itemId.toString)

    val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
    val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
    val url = s"${js.url}?apiClient=${apiClient.clientId}&options=$encrypted"



    val out = for {
      jsResult <- getResultFor(FakeRequest(js.method, url))
      cookies <- if(addCookies) Some(cookies(jsResult)) else Some(Cookies(None))
      createSessionResult <- getResultFor(createSessionRequest(createSession,cookies), SEE_OTHER)
    } yield createSessionResult

    out match {
      case Some(r) => {
        Logger.debug(contentAsString(r))
        logger.debug(s"add cookies $addCookies")
        status(r) === expectedStatus
        if(expectedStatus == SEE_OTHER && expectedLocation.isDefined){
          logger.debug(s">> Location: ${headers(r).get("Location")}")

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

    val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
    val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
    val url = s"${js.url}?apiClient=${apiClient.clientId}&options=$encrypted"

    val out = for {
      jsResult <- getResultFor(FakeRequest(js.method, url))
      cookies <- Some(cookies(jsResult))
      createSessionResult <- getResultFor(createSessionRequest(createSession, cookies), SEE_OTHER)
      playerRequest <- loadPlayer(createSessionResult, cookies)
      loadPlayerResult <- getResultFor(playerRequest)
    } yield createSessionResult

    out match {
      case Some(result) => success
      case _ => failure
    }
  }


  "when I load the player js with orgId and options" should {

    "fail if i don't pass in the session" in
      loadJsThenCreateSession(
        SEE_OTHER,
        addCookies = false,
        expectedLocation = Some("/login"))

    "allow me to create a session" in loadJsThenCreateSession()
    "allow me to create a session and load player" in loadJsThenCreateSessionThenLoadPlayer
  }
}
