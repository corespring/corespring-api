package org.corespring.poc.integration.impl.controllers.player

import org.corespring.container.client.actions.{PlayerRequest, ClientHooksActionBuilder}
import org.corespring.container.client.controllers.hooks.PlayerHooks
import play.api.mvc.{Action, Result, AnyContent}

trait PlayerHooksImpl extends PlayerHooks {

  def builder: ClientHooksActionBuilder[AnyContent] = new ClientHooksActionBuilder[AnyContent] {
    def loadComponents(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

    def loadServices(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

    def loadConfig(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))
  }
}
