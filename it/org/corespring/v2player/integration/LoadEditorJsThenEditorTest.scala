package org.corespring.v2player.integration

import org.corespring.container.client.controllers.routes.PlayerLauncher
import org.corespring.it.{ IntegrationHelpers, IntegrationSpecification }
import org.corespring.v2player.integration.scopes.data
import org.slf4j.LoggerFactory
import play.api.mvc._
import play.api.test.FakeRequest
import org.corespring.v2player.integration.actionBuilders.CheckUserAndPermissions.Errors

class LoadEditorJsThenEditorTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.load-editor")

  "when I load the editor js with orgId and encrypted options" should {
    "fail if i don't pass in the session" in new loader(false) {
      status(result) === UNAUTHORIZED
      val err = Errors.noOrgIdAndOptions(FakeRequest("", ""))
      contentAsString(result) === org.corespring.container.client.views.html.error.main(err._1, err._2).toString
    }
    "allow me to load the editor" in new loader(true) {
      status(result) === OK
    }
  }

  class loader(val addCookies: Boolean) extends data {

    lazy val result = {
      val url = urlWithEncryptedOptions(PlayerLauncher.editorJs, apiClient)

      import org.corespring.container.client.controllers.hooks.routes.EditorHooks

      val editItemCall = EditorHooks.editItem(itemId.toString)

      val r = for {
        jsResult <- getResultFor(FakeRequest("GET", url))
        requestCookies <- if (addCookies) Some(cookies(jsResult)) else Some(Cookies(None))
        loadEditorResult <- route(makeRequest(editItemCall, requestCookies))
      } yield loadEditorResult

      r.getOrElse(throw new RuntimeException("no result"))
    }
  }

}
