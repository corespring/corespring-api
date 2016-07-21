package web.controllers

import org.bson.types.ObjectId
import org.corespring.container.client.VersionInfo
import org.corespring.itemSearch.AggregateType.{ ItemType, WidgetType }
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.services.auth.ApiClientService
import org.corespring.services.item.FieldValueService
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.v2.actions.V2ActionsFactory
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.MockFactory
import org.corespring.web.common.controllers.deployment.AssetsLoader
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsArray, Json }
import play.api.test.PlaySpecification
import web.models.WebExecutionContext

import scala.concurrent.ExecutionContext

class MainTest extends Specification with Mockito with PlaySpecification with MockFactory {

  trait scope extends Scope {

    lazy val fieldValueService = {
      val m = mock[FieldValueService]
      m.get returns Some(FieldValue())
      m
    }

    lazy val jsonFormatting = new JsonFormatting {
      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def rootOrgId: ObjectId = ObjectId.get

      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
    }

    lazy val userService = {
      val m = mock[UserService]
      m

    }

    lazy val userSessionOrgIdentity = {
      val m = mock[UserSessionOrgIdentity]
      m
    }

    lazy val playerTokenService = {
      val m = mock[PlayerTokenService]
      m
    }

    lazy val orgService = {
      val m = mock[OrganizationService]
      m
    }

    lazy val itemType = {
      val m = mock[ItemType]
      m.all returns Json.arr()
      m
    }

    lazy val widgetType = {
      val m = mock[WidgetType]
      m.all returns Json.arr()
      m
    }

    lazy val apiClientService = {
      val m = mock[ApiClientService]
      m
    }

    lazy val containerVersion = VersionInfo("version", "commit", "date", "", Json.obj())

    lazy val webExecutionContext = WebExecutionContext(ExecutionContext.global)

    val assetsLoader = mock[AssetsLoader]

    lazy val orgAndOpts = V2ActionsFactory.orgAndOpts
    lazy val actions = V2ActionsFactory.apply()

    val main = new Main(
      actions,
      fieldValueService,
      jsonFormatting,
      userService,
      orgService,
      itemType,
      widgetType,
      containerVersion,
      webExecutionContext,
      playerTokenService,
      assetsLoader)
  }

  "defaultValues" should {

    "return updated widgetTypes from widgetTypes.all" in new scope {
      val result = main.defaultValues.map(s => Json.parse(s)).get
      (result \ "widgetTypes").as[JsArray] === Json.arr()
      val widgets = Json.arr(Json.obj("key" -> "widget", "value" -> "Widget"))
      widgetType.all returns widgets
      val secondResult = main.defaultValues.map(s => Json.parse(s)).get
      (secondResult \ "widgetTypes").as[JsArray] === widgets
    }
  }
}
