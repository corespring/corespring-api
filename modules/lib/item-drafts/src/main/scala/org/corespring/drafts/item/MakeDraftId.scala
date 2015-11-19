package org.corespring.drafts.item

import org.corespring.drafts.errors.{ CantParseDraftId, DraftError }
import org.corespring.drafts.item.models.{ DraftId, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{ Validation }
import scalaz.Scalaz._

trait MakeDraftId {

  /** if the id is a DraftId id string use that, fallback to using the id as an ObjectId */
  def mkDraftId(user: OrgAndUser, id: String): Validation[DraftError, DraftId] = {
    DraftId
      .fromIdString(id, user.org.id)
      .orElse {
        VersionedId(id).map { vid =>
          val userName = user.user.map(_.userName).getOrElse("unknown_user")
          DraftId(vid.id, userName, user.org.id)
        }
      }.toSuccess(CantParseDraftId(id))
  }
}
