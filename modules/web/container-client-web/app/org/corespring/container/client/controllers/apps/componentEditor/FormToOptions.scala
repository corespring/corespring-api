package org.corespring.container.client.controllers.apps.componentEditor

import org.corespring.container.client.controllers.apps.{ ComponentEditorOptions, PreviewRightComponentEditorOptions, TabComponentEditorOptions }
import play.api.mvc.{ AnyContent, Request }

private[controllers] object FormToOptions {
  def apply(request: Request[AnyContent]): (String, ComponentEditorOptions) = {
    request.body.asFormUrlEncoded.map { f =>
      val previewMode = f.get("previewMode").flatMap(_.headOption).find(m => m == "tabs" || m == "preview-right").getOrElse("tabs")

      val uploadUrl = f.get("uploadUrl").flatMap(_.headOption)
      val uploadMethod = f.get("uploadMethod").flatMap(_.headOption)

      val options = if (previewMode == "preview-right") {
        val showPreview: Option[Boolean] = f.get("showPreview").map(_.exists(_ == "true"))

        val previewWidth = f.get("previewWidth").flatMap(_.headOption).flatMap { s =>
          try { Some(s.toInt) } catch {
            case _: Throwable => None
          }
        }

        PreviewRightComponentEditorOptions(showPreview, previewWidth, uploadUrl, uploadMethod)
      } else {
        val activePane = f.get("activePane").flatMap(_.headOption)
        val showNavigation: Option[Boolean] = f.get("showNavigation").map(_.exists(_ == "true"))
        TabComponentEditorOptions(activePane, showNavigation, uploadUrl, uploadMethod)
      }
      previewMode -> options
    }.getOrElse("tabs" -> ComponentEditorOptions.default)
  }
}
