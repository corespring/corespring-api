package org.corespring.drafts

import org.corespring.drafts.errors.{ UserCantCommit, CommitError, CommitsWithSameSrc, DraftError }
import org.joda.time.DateTime

import scalaz.{ Success, Failure }

trait HasVid[VID] {
  def id: VID
}

/** The data src for the draft and it's id/version */
trait Src[VID, DATA] {
  def data: DATA
  protected def dataWithVid: HasVid[VID]
  def id[VID] = dataWithVid.id
}

trait Draft[ID, VID, SRC_DATA] {
  def id: ID
  def src: Src[VID, SRC_DATA]
  /** update the data in the draft */
  def update(data: SRC_DATA): Draft[ID, VID, SRC_DATA]
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
  /** The id and version that the commit went to. */
  def committedId: VID
  /** When */
  def date: DateTime
  /** By who */
  def user: USER
}

/**
 * Operations you can perform on drafts
 */
trait Drafts[ID, VID, SRC, USER, UD <: UserDraft[ID, VID, SRC, USER]] {

  import scalaz.Validation
  /**
   * Creates a draft for the target data.
   */
  def create(id: VID, user: USER, expires: Option[DateTime] = None): Option[UD]

  /**
   * Commit a draft back to the data store.
   */
  def commit(requester: USER)(d: UD, force: Boolean = false): Validation[DraftError, Commit[VID, USER]]
  /** load a draft by its id */
  def load(requester: USER)(id: ID): Option[UD]
  /** save a draft */
  def save(requester: USER)(d: UD): Validation[DraftError, ID]
}

/**
 * Checks if there have been any commits with the same src id/version and fails if there have been.
 */
trait DraftsWithCommitAndCreate[ID, VID, SRC, USER, UD <: UserDraft[ID, VID, SRC, USER], CMT <: Commit[VID, USER]]
  extends Drafts[ID, VID, SRC, USER, UD] {

  import scalaz.Validation

  override def commit(requester: USER)(d: UD, force: Boolean = false): Validation[DraftError, CMT] = {

    if (d.user == requester) {
      val commits = loadCommits(d.src.id)

      if (commits.length > 0 && !force) {
        Failure(CommitsWithSameSrc(commits))
      } else {
        commitData(d)
      }
    } else {
      Failure(UserCantCommit(requester, d.user))
    }
  }

  /**
   * Commit the draft, create a commit and store it for future checks.
   */
  private def commitData(d: UD): Validation[DraftError, CMT] = {
    saveDraftBackToSrc(d) match {
      case Failure(err) => Failure(err)
      case Success(commit) => for {
        _ <- updateDraftSrcId(d, commit.committedId)
        _ <- saveCommit(commit)
      } yield commit
    }
  }

  /**
   * Creates a draft for the target data.
   * //TODO: add expires
   */
  override def create(id: VID, user: USER, expires: Option[DateTime] = None): Option[UD] = {
    if (userCanCreateDraft(id, user)) {
      findLatestSrc(id).flatMap { src =>
        val result = for {
          draft <- mkDraft(id, src, user)
          saved <- save(user)(draft)
        } yield draft

        result.toOption
      }
    } else {
      None
    }
  }

  /**
   * update the draft src id to the new id
   * @param newSrcId
   * @return
   */
  protected def updateDraftSrcId(d: UD, newSrcId: VID): Validation[DraftError, Unit]

  /** Check that the user may create the draft for the given src id */
  protected def userCanCreateDraft(id: VID, user: USER): Boolean

  /**
   * Load commits that have used the same srcId
   * @return
   */
  protected def loadCommits(idAndVersion: VID): Seq[Commit[VID, USER]]

  protected def saveCommit(c: CMT): Validation[CommitError, Unit]

  protected def deleteDraft(d: UD): Validation[DraftError, Unit]

  protected def saveDraftBackToSrc(d: UD): Validation[DraftError, CMT]

  protected def findLatestSrc(id: VID): Option[SRC]

  protected def mkDraft(srcId: VID, src: SRC, user: USER): Validation[DraftError, UD]
}

