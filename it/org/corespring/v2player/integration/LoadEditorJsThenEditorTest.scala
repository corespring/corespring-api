package org.corespring.v2player.integration

import org.corespring.container.client.controllers.routes.PlayerLauncher
import org.corespring.it.{ IntegrationHelpers, IntegrationSpecification }
import org.corespring.v2player.integration.scopes.data
import org.slf4j.LoggerFactory
import org.specs2.execute.Result
import play.api.mvc._
import play.api.test.FakeRequest

class LoadEditorJsThenEditorTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.load-editor")

  "when I load the editor js with orgId and options" should {
    "fail if i don't pass in the session" in loadJsThenLoadEditor(BAD_REQUEST, false)
    "allow me to load the editor" in loadJsThenLoadEditor()
  }

  def loadEditorRequest(call: Call, c: Cookies): Request[AnyContentAsEmpty.type] = {
    val req = FakeRequest(call.method, call.url)
    req.withCookies(c.toSeq: _*)
  }

  class loader(val expectedStatus: Int, val addCookies: Boolean) extends data

  def loadJsThenLoadEditor(
    expectedStatus: Int = OK,
    addCookies: Boolean = true): Result = new loader(expectedStatus, addCookies) {

    val url = getEncryptedOptions(PlayerLauncher.editorJs, apiClient)

    import org.corespring.container.client.controllers.hooks.routes.EditorHooks

    val editItemCall = EditorHooks.editItem(itemId.toString)

    val r = for {
      jsResult <- getResultFor(FakeRequest("GET", url))
      requestCookies <- if (addCookies) Some(cookies(jsResult)) else Some(Cookies(None))
      loadEditorResult <- route(loadEditorRequest(editItemCall, requestCookies))
    } yield loadEditorResult

    r.map {
      result =>
        logger.debug(s"status: ${status(result)}")
        status(result) === expectedStatus
    }.getOrElse(failure("can't load result"))

  }
}
