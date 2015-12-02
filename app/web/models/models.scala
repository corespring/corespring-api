package web.models

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

case class ContainerVersion(json: JsValue)

case class WebExecutionContext(context: ExecutionContext)
