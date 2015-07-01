package org.corespring.drafts.item

import org.corespring.drafts.errors.{ CantParseDraftId, CantParseVersionedId, DraftError }
import org.corespring.drafts.item.models.{ DraftId, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

trait MakeDraftId {

  /** if itemId - the return itemid, user, orgId */
  def mkDraftId(user: OrgAndUser, id: String): Validation[DraftError, DraftId] = {

    val (itemId, name) = {
      if (id.contains("~")) {
        val Array(itemId, name) = id.split("~")
        (itemId, name)
      } else {
        (id, user.user.map(_.userName).getOrElse("unknown_user"))
      }
    }

    for {
      vid <- VersionedId(itemId).toSuccess(CantParseVersionedId(itemId))
    } yield {
      DraftId(vid.id, name, user.org.id)
    }
  }
}
