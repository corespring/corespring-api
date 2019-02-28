package org.corespring.container.client.component

import org.corespring.container.client.controllers.helpers.{ JsonHelper, NameHelper }
import org.corespring.container.components.model.{ ComponentInfo, Interaction, Widget }
import play.api.libs.json.{ JsBoolean, JsObject, JsString, JsValue }

trait ComponentJson {
  def toJson[C <: ComponentInfo](c: C): JsValue
}

class ComponentInfoJson(modulePath: String) extends NameHelper with JsonHelper with ComponentJson {

  private def iconPath(tag: String) = s"$modulePath/icon/$tag"

  override def toJson[C <: ComponentInfo](ci: C) = {

    val tag = tagName(ci.id.org, ci.id.name)

    val icon = ci match {
      case i: Interaction if i.icon.isDefined => Some(JsString(iconPath(tag)))
      case w: Widget if w.icon.isDefined => Some(JsString(iconPath(tag)))
      case _ => None
    }

    partialObj(
      "name" -> Some(JsString(ci.id.name)),
      "title" -> Some(JsString(ci.title.getOrElse(""))),
      "titleGroup" -> Some(JsString(ci.titleGroup.getOrElse(""))),
      "icon" -> icon,
      "released" -> Some(JsBoolean(ci.released)),
      "insertInline" -> Some(JsBoolean(ci.insertInline)),
      "componentType" -> Some(JsString(tag)),
      "defaultData" -> Some(ci.defaultData),
      "configuration" -> (ci.packageInfo \ "external-configuration").asOpt[JsObject])
  }
}
