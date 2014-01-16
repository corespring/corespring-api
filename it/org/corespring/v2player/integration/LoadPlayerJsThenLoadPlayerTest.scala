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

  trait data extends BeforeAfter {
    val orgId = OrganizationHelper.create("org")
    val apiClient = ApiClientHelper.create(orgId)
    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)

    def before: Any = {
      logger.debug("data ready")
    }

    def after: Any = {
      ApiClientHelper.delete(apiClient)
      OrganizationHelper.delete(orgId)
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
    }
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

    val js = playerLauncher.playerJs()
    val createSession = playerHooks.createSessionForItem(itemId.toString)

    val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
    val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
    val url = s"${js.url}?apiClient=${apiClient.clientId}&options=$encrypted"

    def getResultFor[T](request: Request[T], expectedStatus: Int = OK)(implicit writable: Writeable[T]): Option[Future[SimpleResult]] = {
      val result: Option[Future[SimpleResult]] = route(request)
      result.filter {
        r => {
          val s = status(r)
          if (s != expectedStatus) {
            logger.debug(s"status: $s")
            logger.debug(s"content: ${contentAsString(r)}")
          }
          s == expectedStatus
        }
      }
    }

    def createSessionRequest(c: Cookies): Request[AnyContentAsEmpty.type] = {
      val req = FakeRequest(createSession.method, createSession.url)

      if (addCookies) req.withCookies(c.toSeq: _*) else req
    }

    val out = for {
      jsResult <- getResultFor(FakeRequest(js.method, url))
      cookies <- Some(cookies(jsResult))
      createSessionResult <- getResultFor(createSessionRequest(cookies), SEE_OTHER)
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


  "when I load the player js with orgId and options" should {

    "fail if i don't pass in the session" in
      loadJsThenCreateSession(
        SEE_OTHER,
        addCookies = false,
        expectedLocation = Some("/login"))

    "allow me to create a session" in loadJsThenCreateSession()
  }
}
