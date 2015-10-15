package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.drafts.errors.GeneralError
import org.corespring.models.item.FieldValue
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.json.JsonFormatting
import org.corespring.v2.auth.models.MockFactory
import org.corespring.v2.errors.Errors.generalError
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.{ ExecutionContext, Await, Future }
import scala.concurrent.duration._

private[player] class V2PlayerIntegrationSpec extends Specification with Mockito with MockFactory with PlaySpecification with NoTimeConversions {

  implicit val fakeRequest = FakeRequest("", "")

  val containerExecutionContext = new ContainerExecutionContext(ExecutionContext.global)

  implicit val ec = containerExecutionContext.context

  def TestError(msg: String) = generalError(msg)

  def DraftTestError(msg: String) = GeneralError(msg)

  private[corespring] trait StubJsonFormatting {

    val jsonFormatting = new JsonFormatting {
      override def fieldValue: FieldValue = FieldValue()

      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def rootOrgId: ObjectId = ObjectId.get

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
    }
  }

}

