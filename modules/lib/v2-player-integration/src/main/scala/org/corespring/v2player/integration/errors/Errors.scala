package org.corespring.v2player.integration.errors

import play.api.mvc.{ AnyContent, Request }
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

sealed abstract class V2Error(val code: Int, val message: String)

object Errors {

  import play.api.http.Status._

  case class generalError(c: Int, msg: String) extends V2Error(c, msg)

  object noJson extends V2Error(BAD_REQUEST, "No json in request body")

  case class propertyNotFoundInJson(name: String) extends V2Error(BAD_REQUEST, s"can't find $name in request body")

  case class noOrgIdAndOptions(request: Request[AnyContent]) extends V2Error(UNAUTHORIZED, s"can not load orgId and PlayerOptions from session: ${request.session.data}")

  case class noCollectionIdForItem(vid:VersionedId[ObjectId]) extends V2Error(BAD_REQUEST, s"This item has no collection id $vid")

  case class orgCantAccessCollection(orgId: ObjectId, collectionId: String) extends V2Error(UNAUTHORIZED, s"The org: $orgId can't access collection: $collectionId")

  case object default extends V2Error(UNAUTHORIZED, "Failed to grant access")

  case class cantLoadSession(id: String) extends V2Error(NOT_FOUND, s"Can't load session with id $id")

  case object cantParseItemId extends V2Error(BAD_REQUEST, "Can't parse itemId")

  case class cantFindItemWithId(vid: VersionedId[ObjectId]) extends cantFindById("item", vid.toString())

  case class cantFindOrgWithId(orgId: ObjectId) extends cantFindById("org", orgId.toString)

  abstract class cantFindById(name: String, id: String) extends V2Error(NOT_FOUND, s"Can't find $name with id $id")

}
