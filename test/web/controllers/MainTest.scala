package web.controllers

import org.bson.types.ObjectId
import org.corespring.itemSearch.AggregateType.{ WidgetType, ItemType }
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.services.item.FieldValueService
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsArray, Json }
import play.api.test.PlaySpecification
import web.models.{ WebExecutionContext, ContainerVersion }

import scala.concurrent.ExecutionContext

class MainTest extends Specification with Mockito with PlaySpecification {

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
      val m = mock[UserSessionOrgIdentity[OrgAndOpts]]
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
    lazy val containerVersion = ContainerVersion(Json.obj())

    lazy val webExecutionContext = WebExecutionContext(ExecutionContext.global)

    val main = new Main(
      fieldValueService,
      jsonFormatting,
      userService,
      orgService,
      itemType,
      widgetType,
      containerVersion,
      webExecutionContext,
      playerTokenService,
      userSessionOrgIdentity)
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
