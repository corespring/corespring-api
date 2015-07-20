package org.corespring.v2.api

import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.services.item.ItemIndexService
import play.api.libs.json.{ JsString, JsArray, Json }

import scalaz.{ Failure, Success }

trait FieldValuesApi extends V2Api {

  def itemIndexService: ItemIndexService

  import Organization._

  def get(field: String) = futureWithIdentity { (identity, _) =>
    itemIndexService.distinct(field,
      identity.org.contentcolls.accessible.map(_.collectionId.toString)).map(_ match {
        case Success(contributors) => Ok(Json.prettyPrint(JsArray(contributors.map(JsString))))
        case Failure(error) => InternalServerError(error.getMessage)
      })
  }

}
