package org.corespring.api.v2.errors

import play.api.mvc.RequestHeader
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId

sealed abstract class V2ApiError(val code: Int, val message: String)

object Errors {

  import play.api.http.Status._

  case class generalError(c: Int, msg: String) extends V2ApiError(c, msg)

  object noJson extends V2ApiError(BAD_REQUEST, "No json in request body")

  object errorSaving extends V2ApiError(BAD_REQUEST, "Error saving")

  case class propertyNotFoundInJson(name: String) extends V2ApiError(BAD_REQUEST, s"can't find $name in request body")

  case class noOrgIdAndOptions(request: RequestHeader) extends V2ApiError(UNAUTHORIZED, s"can not load orgId and PlayerOptions from session: ${request.session.data}")

  case class noCollectionIdForItem(vid: VersionedId[ObjectId]) extends V2ApiError(BAD_REQUEST, s"This item has no collection id $vid")

  case class orgCantAccessCollection(orgId: ObjectId, collectionId: String) extends V2ApiError(UNAUTHORIZED, s"The org: $orgId can't access collection: $collectionId")

  case object default extends V2ApiError(UNAUTHORIZED, "Failed to grant access")

  case class cantLoadSession(id: String) extends V2ApiError(NOT_FOUND, s"Can't load session with id $id")

  case object cantParseItemId extends V2ApiError(BAD_REQUEST, "Can't parse itemId")

  case class cantFindItemWithId(vid: VersionedId[ObjectId]) extends cantFindById("item", vid.toString())

  case class cantFindOrgWithId(orgId: ObjectId) extends cantFindById("org", orgId.toString)

  abstract class cantFindById(name: String, id: String) extends V2ApiError(NOT_FOUND, s"Can't find $name with id $id")

}
