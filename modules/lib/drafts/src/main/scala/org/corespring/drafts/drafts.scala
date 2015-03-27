package org.corespring.drafts

import org.corespring.drafts.errors.{ UserCantCommit, CommitError, CommitsWithSameSrc, DraftError }
import org.joda.time.DateTime

import scalaz.{ Success, Failure }

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
  def created: DateTime //= DateTime.now
  def expires: DateTime
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

  import scalaz.Validation
  /**
   * Creates a draft for the target data.
   */
  def create(id: SRC_ID, user: USER, expires: Option[DateTime] = None): Option[UD]

  /**
   * Commit a draft back to the data store
   */
  def commit(requester: USER)(d: UD, force: Boolean = false): Validation[DraftError, Commit[SRC_ID, SRC_VERSION, USER]]
  /** load a draft by its id */
  def load(requester: USER)(id: ID): Option[UD]
  /** save a draft */
  def save(requester: USER)(d: UD): Validation[DraftError, ID]
}

/**
 * Checks if there have been any commits with the same src id/version and fails if there have been.
 */
trait DraftsWithCommitAndCreate[ID, SRC_ID, SRC_VERSION, SRC, USER, UD <: UserDraft[ID, SRC_ID, SRC_VERSION, SRC, USER], CMT <: Commit[SRC_ID, SRC_VERSION, USER]]
  extends Drafts[ID, SRC_ID, SRC_VERSION, SRC, USER, UD] {

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
    saveDraftSrcAsNewVersion(d) match {
      case Failure(err) => Failure(err)
      case Success(commit) => for {
        _ <- saveCommit(commit)
        _ <- deleteDraft(d)
      } yield commit
    }
  }

  /**
   * Creates a draft for the target data.
   * //TODO: add expires
   */
  override def create(id: SRC_ID, user: USER, expires: Option[DateTime] = None): Option[UD] = {
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

  /** Check that the user may create the draft for the given src id */
  protected def userCanCreateDraft(id: SRC_ID, user: USER): Boolean

  /**
   * Load commits that have used the same srcId
   * @return
   */
  protected def loadCommits(idAndVersion: IdAndVersion[SRC_ID, SRC_VERSION]): Seq[Commit[SRC_ID, SRC_VERSION, USER]]

  protected def saveCommit(c: CMT): Validation[CommitError, Unit]

  protected def deleteDraft(d: UD): Validation[DraftError, Unit]

  protected def saveDraftSrcAsNewVersion(d: UD): Validation[DraftError, CMT]

  protected def findLatestSrc(id: SRC_ID): Option[SRC]

  protected def mkDraft(srcId: SRC_ID, src: SRC, user: USER): Validation[DraftError, UD]
}

