package org.corespring.poc.integration.impl.controllers.player

import org.corespring.container.client.actions.{SubmitAnswersRequest, FullSessionRequest, SessionActionBuilder}
import org.corespring.container.client.controllers.resources.Session
import play.api.mvc.{Action, Result, AnyContent}

trait ClientSessionImpl extends Session{
  def builder: SessionActionBuilder[AnyContent] = new SessionActionBuilder[AnyContent] {
    def submitAnswers(id: String)(block: (SubmitAnswersRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

    def loadEverything(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

    def load(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))
  }
}
