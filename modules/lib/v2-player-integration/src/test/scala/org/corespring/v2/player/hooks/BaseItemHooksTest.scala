package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.R
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ VirtualFile, Resource }
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class BaseItemHooksTest extends V2PlayerIntegrationSpec {

  trait scope extends Scope with StubJsonFormatting with BaseItemHooks {

    val collectionId = ObjectId.get.toString
    val item = Item(collectionId = collectionId)

    protected var updatedItem: Item = null

    override protected def update(id: String, json: JsValue, updateFn: (Item, JsValue) => Item)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
      updatedItem = updateFn(item, json)
      Future(Right(Json.obj()))(ec)
    }

    override def delete(id: String)(implicit h: RequestHeader): R[JsValue] = Future {
      Left(500 -> "not implemented")
    }(ec)

    override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
      Left(500 -> "not implemented")
    }(ec)

    override def containerContext: ContainerExecutionContext = BaseItemHooksTest.this.containerExecutionContext
  }

  "saveCollectionId" should {

    "update the collectionId" in new scope {
      saveCollectionId("id", "new-id")
      updatedItem.collectionId must_== "new-id"
    }
  }

  "saveXhtml" should {
    "update the xhtml" in new scope {
      saveXhtml("id", "<div>hi</div>")
      updatedItem.playerDefinition.get.xhtml must_== "<div>hi</div>"
    }
  }

  "saveSupportingMaterials" should {
    "update the supporting materials" in new scope {
      val materials = Seq(Resource(id = Some(ObjectId.get),
        name = "Rubric",
        files = Seq(VirtualFile("index.html", "text/html", false, "hi"))))

      import jsonFormatting.formatResource
      val materialsJson = Json.toJson(materials)
      saveSupportingMaterials("id", materialsJson)
      updatedItem.supportingMaterials must_== materials
    }
  }

  "saveCustomScoring" should {
    "update custom scoring" in new scope {
      saveCustomScoring("id", "custom")
      updatedItem.playerDefinition.get.customScoring must_== Some("custom")
    }
  }

  "saveComponents" should {
    "update components" in new scope {
      val json = Json.obj("1" -> Json.obj("componentType" -> "blah"))
      saveComponents("id", json)
      updatedItem.playerDefinition.get.components must_== json
    }
  }

  "saveSummaryFeedback" should {
    "update summaryFeedback" in new scope {
      saveSummaryFeedback("id", "feedback")
      updatedItem.playerDefinition.get.summaryFeedback must_== "feedback"
    }
  }

  "saveProfile" should {
    "update taskInfo.title" in new scope {

      val json = Json.obj("taskInfo" ->
        Json.obj("title" -> "hi"))
      saveProfile("id", json)
      updatedItem.taskInfo.get.title must_== (json \ "taskInfo" \ "title").asOpt[String]
    }

    "update standards" in new scope {
      val json = Json.parse("""{"standards":[ {"dotNotation":"dotNotation"}]}""")
      saveProfile("id", json)
      updatedItem.standards(0) must_== "dotNotation"
    }
  }
}
