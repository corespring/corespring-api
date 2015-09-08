package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.{ Subject, Standard }
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import play.api.mvc.RequestHeader
import play.api.test.PlaySpecification

import scala.concurrent.ExecutionContext
import scalaz.{ Validation }

private[api] trait V2ApiSpec extends PlaySpecification with Mockito with MockFactory {

  val jsonFormatting = new JsonFormatting {
    override def fieldValue: FieldValue = ???

    override def findStandardByDotNotation: (String) => Option[Standard] = ???

    override def countItemsInCollection(collectionId: ObjectId): Long = ???

    override def rootOrgId: ObjectId = ???

    override def findSubjectById: (ObjectId) => Option[Subject] = ???
  }

  def testError = generalError("test-error")
}

private[api] trait V2ApiScope {

  implicit val v2ApiContext = V2ApiExecutionContext(ExecutionContext.global)

  def orgAndOpts: Validation[V2Error, OrgAndOpts]

  protected def getOrgAndOptionsFn = (request: RequestHeader) => orgAndOpts
}
