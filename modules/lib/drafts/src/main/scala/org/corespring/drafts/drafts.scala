package org.corespring.drafts

import org.corespring.drafts.errors.{ CommitError, CommitsWithSameSrc, DraftError }
import org.joda.time.DateTime

trait IdAndVersion[ID, VERSION] {
  def id: ID
  def version: VERSION
}

/** The data src for the draft and it's id/version */
trait Src[DATA, ID, VERSION] {
  def data: DATA
  def id: IdAndVersion[ID, VERSION]
}

trait Draft[ID, SRC_ID, SRC_VERSION, SRC_DATA] {
  def id: ID
  def src: Src[SRC_DATA, SRC_ID, SRC_VERSION]
  /** update the data in the draft */
  def update(data: SRC_DATA): Draft[ID, SRC_ID, SRC_VERSION, SRC_DATA]
}

/** a draft created by a user */
trait UserDraft[ID, SRC_ID, SRC_VERSION, SRC_DATA, USER]
  extends Draft[ID, SRC_ID, SRC_VERSION, SRC_DATA] {
  def user: USER
}

/** A record of a draft that was committed as data */
trait Commit[ID, VERSION, USER] {
  /** The id and version used as the src for the draft */
  def srcId: IdAndVersion[ID, VERSION]
  /** The id and version that the commit went to. */
  def committedId: IdAndVersion[ID, VERSION]
  /** When */
  def date: DateTime
  /** By who */
  def user: USER
}

/**
 * Operations you can perform on drafts
 */
trait Drafts[ID, SRC_ID, SRC_VERSION, SRC, USER, UD <: UserDraft[ID, SRC_ID, SRC_VERSION, SRC, USER]] {

  /**
   * Creates a draft for the target data.
   */
  def create(id: SRC_ID, user: USER): Option[UD]

  /**
   * Commit a draft back to the data store
   */
  def commit(d: UD, force: Boolean = false): Either[DraftError, Commit[SRC_ID, SRC_VERSION, USER]]
  /** load a draft by its id */
  def load(id: ID): Option[UD]
  /** save a draft */
  def save(d: UD): Either[DraftError, ID]
}

/**
 * Checks if there have been any commits with the same src id/version and fails if there have been.
 */
trait DraftsWithCommitAndCreate[ID, SRC_ID, SRC_VERSION, SRC, USER, UD <: UserDraft[ID, SRC_ID, SRC_VERSION, SRC, USER], CMT <: Commit[SRC_ID, SRC_VERSION, USER]]
  extends Drafts[ID, SRC_ID, SRC_VERSION, SRC, USER, UD] {

  override final def commit(d: UD, force: Boolean = false): Either[DraftError, Commit[SRC_ID, SRC_VERSION, USER]] = {
    val commits = loadCommits(d.src.id)

    if (commits.length > 0 && !force) {
      Left(CommitsWithSameSrc(commits))
    } else {
      commitData(d)
    }
  }

  /**
   * commit the draft, create a commit and store it
   * for future checks.
   * @param d
   * @return
   */
  private def commitData(d: UD): Either[DraftError, CMT] = {
    saveDraftSrcAsNewVersion(d) match {
      case Left(err) => Left(err)
      case Right(commit) => {
        saveCommit(commit)
        deleteDraft(d)
        Right(commit)
      }
    }
  }

  /**
   * Creates a draft for the target data.
   */
  override final def create(id: SRC_ID, user: USER): Option[UD] = {
    findLatestSrc(id).flatMap { src =>
      val draft = mkDraft(id, src, user)
      save(draft) match {
        case Left(_) => None
        case Right(id) => Some(draft)
      }
    }
  }

  /**
   * Load commits that have used the same srcId
   * @return
   */
  protected def loadCommits(idAndVersion: IdAndVersion[SRC_ID, SRC_VERSION]): Seq[Commit[SRC_ID, SRC_VERSION, USER]]

  protected def saveCommit(c: CMT): Either[CommitError, Unit]

  protected def deleteDraft(d: UD): Either[DraftError, Unit]

  protected def saveDraftSrcAsNewVersion(d: UD): Either[DraftError, CMT]

  protected def findLatestSrc(id: SRC_ID): Option[SRC]

  protected def mkDraft(srcId: SRC_ID, src: SRC, user: USER): UD
}

