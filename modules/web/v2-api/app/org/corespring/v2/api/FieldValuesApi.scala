package org.corespring.v2.api

import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.json.JsonFormatting
import org.corespring.services.StandardService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsArray, JsString, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.{ Future, ExecutionContext }
import scalaz.{ Failure, Success, Validation }

class FieldValuesApi(
  indexService: ItemIndexService,
  v2ApiContext: V2ApiExecutionContext,
  standardService: StandardService,
  jsonFormatting: JsonFormatting,
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

  def domain = futureWithIdentity { (identity, _) =>
    import jsonFormatting.writeStandardDomains
    standardService.domains.map { sd =>
      Ok(Json.toJson(sd))
    }
  }

  def subject = futureWithIdentity { (identity, _) =>
    Future(NotImplemented)
  }

  def standard = futureWithIdentity { (identity, _) =>
    Future(NotImplemented)
  }

}
