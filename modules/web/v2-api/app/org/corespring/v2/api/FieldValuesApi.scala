package org.corespring.v2.api

import play.api.libs.json.{ JsString, JsArray, Json }
import org.corespring.itemSearch.ItemIndexService
import scalaz.{ Failure, Success }

trait FieldValuesApi extends V2Api {

  def indexService: ItemIndexService

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
