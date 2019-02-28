package org.corespring.container.client.controllers.apps

import org.corespring.container.client.controllers.resources.SingleComponent
import play.api.libs.json._

trait TemplateParams {
  def appName: String

  def js: Seq[String]

  def css: Seq[String]

  def componentNgModules: Seq[String]

  def ngServiceLogic: String

  def toJadeParams: Map[String, Object] = {
    Map(
      "appName" -> appName,
      "js" -> js.toArray,
      "css" -> css.toArray,
      "componentNgModules" -> s"${componentNgModules.map { m => s"'$m'" }.mkString(",")}",
      "ngServiceLogic" -> ngServiceLogic)
  }
}

case class EditorClientOptions(
  debounceInMillis: Long,
  uploadAudioMaxSizeKb: Long,
  uploadImageMaxSizeKb: Long,
  staticPaths: JsObject) {
  def toJson = Json.format[EditorClientOptions].writes(this)
}

trait ComponentEditorOptions {
  def uploadUrl: Option[String]
  def uploadMethod: Option[String]
  def toJson: JsValue
}

case class PreviewRightComponentEditorOptions(
  showPreview: Option[Boolean],
  previewWidth: Option[Int],
  uploadUrl: Option[String],
  uploadMethod: Option[String],
  singleComponentKey: String = SingleComponent.Key)
  extends ComponentEditorOptions {
  override def toJson = Json.format[PreviewRightComponentEditorOptions].writes(this)
}

case class TabComponentEditorOptions(activePane: Option[String],
  showNavigation: Option[Boolean],
  uploadUrl: Option[String],
  uploadMethod: Option[String],
  singleComponentKey: String = SingleComponent.Key)
  extends ComponentEditorOptions {
  override def toJson = Json.format[TabComponentEditorOptions].writes(this)
}

object ComponentEditorOptions {
  def default = TabComponentEditorOptions(None, None, None, None)
}

case class PlayerTemplateParams(
  appName: String,
  js: Seq[String],
  css: Seq[String],
  componentNgModules: Seq[String],
  ngServiceLogic: String,
  showControls: Boolean,
  sessionJson: JsValue,
  versionInfo: JsValue,
  useNewRelicRum: Boolean,
  newRelicRumConfig: JsValue,
  warnings: Seq[String] = Seq.empty) extends TemplateParams {
  override def toJadeParams = {
    super.toJadeParams ++ Map(
      "showControls" -> new java.lang.Boolean(showControls),
      "sessionJson" -> Json.stringify(sessionJson),
      "versionInfo" -> Json.stringify(versionInfo),
      "useNewRelicRum" -> new java.lang.Boolean(useNewRelicRum),
      "newRelicRumConfig" -> Json.stringify(newRelicRumConfig),
      "warnings" -> Json.stringify(JsArray(warnings.map(JsString(_)))))
  }
}
