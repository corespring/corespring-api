package org.corespring.container.client.component

import org.corespring.container.components.model.dependencies.ComponentMaker
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ComponentInfoJsonTest extends Specification with ComponentMaker {

  val interaction = uiComp("comp", Nil).copy(
    title = Some("title"),
    titleGroup = Some("group"),
    icon = Some(Array.empty),
    defaultData = Json.obj("a" -> "b"),
    packageInfo = Json.obj("external-configuration" -> Json.obj("config" -> "a")))

  "toJson" should {

    "create json" in {

      val componentJson = new ComponentInfoJson("path")
      componentJson.toJson(interaction) must_== Json.obj(
        "name" -> interaction.id.name,
        "title" -> interaction.title,
        "titleGroup" -> interaction.titleGroup,
        "icon" -> s"path/icon/${interaction.componentType}",
        "released" -> interaction.released,
        "insertInline" -> interaction.insertInline,
        "componentType" -> interaction.componentType,
        "defaultData" -> interaction.defaultData,
        "configuration" -> Json.obj("config" -> "a"))
    }
  }

}
