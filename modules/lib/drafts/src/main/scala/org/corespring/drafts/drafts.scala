package org.corespring.drafts

import org.corespring.drafts.errors._
import org.joda.time.DateTime

import scalaz.{ Failure }
import scalaz.Scalaz._

/** The data src for the draft and it's id/version */
trait Src[VID, DATA] {

  protected trait HasVid[VID] {
    def id: VID
  }

  def data: DATA
  protected def dataWithVid: HasVid[VID]
  def id[VID] = dataWithVid.id
}

trait Draft[ID, VID, SRC_DATA] {
  def id: ID
  /** the data from which draft was created */
  def parent: Src[VID, SRC_DATA]
  /** the change to that data */
  def change: Src[VID, SRC_DATA]
  def mkChange(data: SRC_DATA): Draft[ID, VID, SRC_DATA]
  def created: DateTime //= DateTime.now
  def expires: DateTime
}

/** a draft created by a user */
trait UserDraft[ID, VID, SRC_DATA, USER]
  extends Draft[ID, VID, SRC_DATA] {
  def user: USER
}

/** A record of a draft that was committed as data */
trait Commit[VID, USER] {
  /** The id and version used as the src for the draft */
  def srcId: VID
  /** When */
  def date: DateTime
  /** By who */
  def user: USER
}

/**
 * Operations you can perform on drafts
 */
trait Drafts[ID, VID, SRC, USER, UD <: UserDraft[ID, VID, SRC, USER], CMT <: Commit[VID, USER], OOD <: DraftIsOutOfDate[ID, VID, SRC]] {

  import scalaz.Validation

  /**
   * Creates a draft for the target data.
   */
  def create(id: VID, user: USER, expires: Option[DateTime] = None): Validation[DraftError, UD]

  /**
   * Check that the draft src matches the latest src,
   * so that a commit is possible.
   */
  def getLatestSrc(d: UD): Option[Src[VID, SRC]]

  def draftIsOutOfDate(d: UD, src: Src[VID, SRC]): OOD

  def hasSrcChanged(a: SRC, b: SRC): Boolean

  /**
   * Commit a draft back to the data store.
   */
  final def commit(requester: USER)(d: UD, force: Boolean = false): Validation[DraftError, CMT] = {

    if (!hasSrcChanged(d.parent.data, d.change.data)) {
      Failure(NothingToCommit(d.id))
    } else if (d.user != requester) {
      Failure(UserCantCommit(requester, d.user))
    } else {
      for {
        latest <- getLatestSrc(d).toSuccess(CantFindLatestSrc(d.id))
        result <- if (hasSrcChanged(d.parent.data, latest.data) && !force) {
          Failure(draftIsOutOfDate(d, latest))
        } else {
          copyDraftToSrc(d)
        }
      } yield result
    }
  }

  protected def copyDraftToSrc(d: UD): Validation[DraftError, CMT]
  protected def copySrcToDraft(src: SRC, draft: UD): Validation[DraftError, UD]

  /** load a draft for the src <VID> for that user*/
  def loadOrCreate(requester: USER)(id: ID, ignoreConflicts: Boolean = false): Validation[DraftError, UD]

  /** save a draft */
  def save(requester: USER)(d: UD): Validation[DraftError, ID]
}
