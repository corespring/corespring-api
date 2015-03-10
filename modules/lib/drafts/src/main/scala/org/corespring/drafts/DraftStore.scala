package org.corespring.drafts

import org.corespring.drafts.errors.{ DataWithSameSrc, DraftError }
import org.corespring.drafts.models.{ UserDraft, CommittedDraft, DraftSrc, Draft }

trait DraftStore[USER, ID, VERSION, DATA, UD <: UserDraft[DATA, USER, ID, VERSION]] {
  type DraftAndSrc = Draft[DraftSrc[ID, VERSION], DATA]

  def loadDataAndVersion(id: ID): (DATA, VERSION)

  def saveData(id: ID, user: USER, data: DATA, src: DraftSrc[ID, VERSION]): Either[DraftError, CommittedDraft[USER, ID, VERSION]]

  def loadCommittedDrafts(id: ID): Seq[CommittedDraft[USER, ID, VERSION]]

  def mkUserDraft(data: DATA, user: USER, src: DraftSrc[ID, VERSION]): UD

  def createDraft(id: ID, user: USER): UD = {
    val (data, version) = loadDataAndVersion(id)
    mkUserDraft(data, user, DraftSrc[ID, VERSION](id, version))
  }

  def commitDraft(id: ID,
    userDraft: UserDraft[DATA, USER, ID, VERSION],
    ignoreExistingDataWithSameSrc: Boolean = false): Either[DraftError, CommittedDraft[USER, ID, VERSION]] = {

    val commits = loadCommittedDrafts(id)
    val withSameSrc = commits.filter(_.src == userDraft.draftSrc)

    if (withSameSrc.length > 0 && !ignoreExistingDataWithSameSrc) {
      Left(DataWithSameSrc(withSameSrc))
    } else {
      saveData(id, userDraft.user, userDraft.data, userDraft.draftSrc)
    }
  }
}

