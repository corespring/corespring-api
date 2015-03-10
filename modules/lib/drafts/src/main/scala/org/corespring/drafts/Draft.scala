package org.corespring.drafts

import org.joda.time.DateTime

trait DraftStore[USER, ID, VERSION, DATA] {
  type DraftAndSrc = Draft[DraftSrc[ID, VERSION], DATA]

  def loadDataAndVersion(id: ID): (DATA, VERSION)

  def saveData(id: ID, user: USER, data: DATA, src: DraftSrc[ID, VERSION]): Either[DraftError, CommittedDraft[USER, ID, VERSION]]

  def loadCommittedDrafts(id: ID): Seq[CommittedDraft[USER, ID, VERSION]]

  def createDraft(id: ID, user: USER): UserDraft[DATA, USER, ID, VERSION] = {
    val (data, version) = loadDataAndVersion(id)
    UserDraft[DATA, USER, ID, VERSION](data, user, DraftSrc[ID, VERSION](id, version))
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

trait Draft[SRC, DATA] {
  def data: DATA
  def src: Option[SRC]
  def update(d: DATA): Draft[SRC, DATA]
}

case class CommittedDraft[USER, ID, VERSION](dataId: ID, user: USER, src: DraftSrc[ID, VERSION], created: DateTime)

case class DraftSrc[ID, VERSION](id: ID, version: VERSION)

case class UserDraft[DATA, USER, ID, VERSION](data: DATA, user: USER, draftSrc: DraftSrc[ID, VERSION])
  extends Draft[DraftSrc[ID, VERSION], DATA] {
  override def update(d: DATA): UserDraft[DATA, USER, ID, VERSION] = this.copy(data = d)

  override def src: Option[DraftSrc[ID, VERSION]] = Some(draftSrc)
}

case class InitialDraft[ID, VERSION, DATA](data: DATA)
  extends Draft[DraftSrc[ID, VERSION], DATA] {
  override def src: Option[DraftSrc[ID, VERSION]] = None

  override def update(d: DATA): InitialDraft[ID, VERSION, DATA] = this.copy(data = d)
}

sealed abstract class DraftError(msg: String)

case class SaveDataFailed(msg: String) extends DraftError(msg)

case class DataWithSameSrc[DATA, USER, ID, VERSION](provenances: Seq[CommittedDraft[USER, ID, VERSION]])
  extends DraftError("There are existing data items that come from the same src")
