package org.corespring.v2.api

import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsString, JsArray, Json }
import org.corespring.itemSearch.ItemIndexService
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext
import scalaz.{ Validation, Failure, Success }

class FieldValuesApi(
  indexService: ItemIndexService,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  def contributors() = get(Keys.contributor)
  def gradeLevels() = get(Keys.gradeLevel)

  private object Keys {
    val contributor = "contributorDetails.contributor"
    val gradeLevel = "taskInfo.gradeLevel"
  }

  private def get(field: String) = futureWithIdentity { (identity, _) =>
    indexService.distinct(field,
      identity.org.accessibleCollections.map(_.collectionId.toString)).map(_ match {
        case Success(contributors) => Ok(Json.prettyPrint(JsArray(contributors.map(JsString))))
        case Failure(error) => InternalServerError(error.getMessage)
      })
  }

}
