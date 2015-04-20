package org.corespring.drafts.item

import org.corespring.drafts.errors.{ CantParseDraftId, CantParseVersionedId, DraftError }
import org.corespring.drafts.item.models.{ DraftId, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

trait MakeDraftId {
  def mkDraftId(user: OrgAndUser, id: String): Validation[DraftError, DraftId] = {
    for {
      arr <- if (id.contains("~"))
        Success(id.split("~"))
      else
        Failure(CantParseDraftId(id))
      vid <- VersionedId(arr(0)).toSuccess(CantParseVersionedId(arr(0)))
    } yield {
      val Array(_, name) = arr
      DraftId(vid.id, user.user.map(_.userName).getOrElse(name), user.org.id)
    }
  }
}
