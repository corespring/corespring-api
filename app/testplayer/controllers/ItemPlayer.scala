package testplayer.controllers

import org.xml.sax.SAXParseException
import xml.Elem
import org.bson.types.ObjectId
import controllers.auth.BaseApi
import models._
import common.controllers.{SimpleJsRoutes, QtiResource, AssetResource, ItemResources}
import itemSession.ItemSession
import play.api.mvc._
import quiz.basic.Quiz
import testplayer.models.ExceptionMessage
import scala.Some
import play.api._
import templates.Html
import qti.models.RenderingMode._


object ItemPlayer extends BaseApi with AssetResource with QtiResource with QtiRenderer with SimpleJsRoutes {


  type TemplateParams = (String, String, Boolean, String, String)

  def javascriptRoutes = Action {
    implicit request =>

      import api.v1.routes.javascript._

      Ok(
      createSimpleRoutes("PlayerRoutes",
          ItemSessionApi.update,
          ItemSessionApi.get,
          ItemSessionApi.create,
          ItemSessionApi.aggregate
        )
      ).as("text/javascript")
  }

  def renderItemBySessionId(sessionId: String, printMode: Boolean = false) = {
    callRenderBySessionId(
      _renderItem(_, _, previewEnabled = false, sessionSettings = "", sessionId = sessionId),
      sessionId,
      if (printMode) Printing else Web)
  }

  def previewItemBySessionId(sessionId: String, printMode: Boolean = false) = {
    callRenderBySessionId(
      _renderItem(_, _, previewEnabled = true, sessionSettings = "", sessionId = sessionId),
      sessionId,
      if (printMode) Printing else Web)
  }

  private def callRenderBySessionId(renderFn: (String, RenderingMode) => Action[AnyContent], id: String, renderMode: RenderingMode) = {
    ItemSession.findOneById(new ObjectId(id)) match {
      case Some(session) => {
        renderFn(session.itemId.toString, renderMode)
      }
      case _ => throw new RuntimeException("Can't find item session: " + id)
    }
  }

  def previewItem(itemId: String, printMode: Boolean = false, sessionSettings: String = "") =
    _renderItem(itemId, if (printMode) Printing else Web, previewEnabled = !printMode, sessionSettings = sessionSettings)

  def renderItem(itemId: String, printMode: Boolean = false, sessionSettings: String = "") =
    _renderItem(itemId, if (printMode) Printing else Web, sessionSettings = sessionSettings)

  def renderAsIframe(itemId: String) = {
    _renderItem(itemId, template = PlayerTemplates.iframed)
  }

  def renderAsInstructor(sessionId: String) = {
    callRenderBySessionId(
      _renderItem(_, _, previewEnabled = false, sessionSettings = "", sessionId = sessionId, template = PlayerTemplates.instructor),
      sessionId,
      renderMode = Instructor
      )
  }


  def renderQuizAsAggregate(quizId: String, itemId: String) = ApiAction { request =>
    Quiz.findOneById(new ObjectId(quizId)) match {
      case Some(q) =>
        try {
          getItemXMLByObjectId(itemId, request.ctx.organization) match {
            case Some(xmlData: Elem) =>
              val finalXml = prepareQti(xmlData, Aggregate)
              Ok(testplayer.views.html.aggregatePlayer(itemId, finalXml, quizId, common.mock.MockToken))
            case None =>
              NotFound("not found")
          }
        } catch {
          case e: SAXParseException => {
            val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
            Ok(testplayer.views.html.itemPlayerError(errorInfo))
          }
        }
      case _ => NotFound
    }

  }

  private object PlayerTemplates {
    def default(params: TemplateParams): play.api.templates.Html = {
      testplayer.views.html.itemPlayer(params._1, params._2, params._3, params._4, params._5)
    }

    def iframed(params: TemplateParams): play.api.templates.Html = {
      testplayer.views.html.iframedPlayer(params._1, params._2, params._3, params._4, params._5)
    }

    def instructor(params: TemplateParams): play.api.templates.Html = {
      testplayer.views.html.itemPlayer(params._1, params._2, params._3, params._4, params._5)
    }
  }


  private def _renderItem(itemId: String,
                          renderMode: RenderingMode = Web,
                          previewEnabled: Boolean = false,
                          sessionSettings: String = "",
                          sessionId: String = "",
                          template: TemplateParams => Html = PlayerTemplates.default) = ApiAction {
    request =>
      try {
        getItemXMLByObjectId(itemId, request.ctx.organization) match {
          case Some(xmlData: Elem) =>

            val finalXml = prepareQti(xmlData, renderMode)

            if (Play.isDev(play.api.Play.current) && request.token == null) {
              println("Mock Token Session")
              Ok(template(itemId, finalXml, previewEnabled, sessionId, common.mock.MockToken))
            }
            else {
              Ok(template(itemId, finalXml, previewEnabled, sessionId, common.mock.MockToken))
            }

          case None =>
            NotFound("not found")
        }
      } catch {
        case e: SAXParseException => {
          val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
          Ok(testplayer.views.html.itemPlayerError(errorInfo))
        }
        //case e: Exception => throw new RuntimeException("ItemPlayer.renderItem: " + e.getMessage, e)
      }
  }

}
