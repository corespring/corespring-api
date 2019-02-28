package org.corespring.container.client.views.models

import play.api.libs.json.{ JsValue, Json, Writes }
import play.api.mvc.Call

private object Helpers {
  implicit val CallWrites: Writes[Call] = Json.writes[Call]
}

import Helpers._

case class MainEndpoints(
  load: Call,
  save: Option[Call],
  saveSubset: Call,
  saveConfigXhtmlAndComponents: Call) {

  val json = Json.writes[MainEndpoints].writes(this)
  val jsonString = Json.stringify(json)
}

case class ComponentsAndWidgets(components: JsValue, widgets: JsValue)

case class SessionEndpoints(
  complete: Call,
  getScore: Call,
  loadInstructorData: Call,
  loadItemAndSession: Call,
  loadOutcome: Call,
  reopen: Call,
  reset: Call,
  save: Call) {

  protected val json = Json.writes[SessionEndpoints].writes(this)

  val jsonString = Json.stringify(json)
}

case class SupportingMaterialsEndpoints(
  addAsset: Call,
  create: Call,
  createFromFile: Call,
  delete: Call,
  deleteAsset: Call,
  getAsset: Call,
  updateContent: Call) {

  protected val json = Json.writes[SupportingMaterialsEndpoints].writes(this)

  val jsonString = Json.stringify(json)
}
