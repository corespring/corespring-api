package org.corespring.v2player.integration

import org.corespring.it.{IntegrationHelpers, IntegrationSpecification}
import org.corespring.v2player.integration.errors.Errors.noOrgIdAndOptions
import org.corespring.v2player.integration.scopes.orgWithAccessTokenAndItem
import org.slf4j.LoggerFactory
import play.api.mvc._
import play.api.test.FakeRequest

class LoadEditorTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.load-editor")

  "when I load the editor js with orgId and encrypted options" should {

    "fail if i don't add credentials to the url" in new loader(false) {
      status(result) === UNAUTHORIZED
      val err = noOrgIdAndOptions(FakeRequest("", ""))
      contentAsString(result) === org.corespring.container.client.views.html.error.main(err.code, err.message).toString
    }

    "allow me to load the editor" in new loader(true) {
      status(result) === OK
    }
  }

  class loader(val addCredentials: Boolean) extends orgWithAccessTokenAndItem {

    lazy val result = {
      import org.corespring.container.client.controllers.apps.routes.Editor

      val editItemCall = {
        val call = Editor.editItem(itemId.toString)
        if(addCredentials){
          Call(call.method, urlWithEncryptedOptions(call, apiClient))
        } else call
      }
      route(makeRequest(editItemCall, Cookies(None))).getOrElse(throw new RuntimeException("Error calling route"))
    }
  }

}
