package org.corespring.drafts

import org.corespring.drafts.errors.{ CommitsWithSameSrc, DraftError }
import org.joda.time.DateTime

/**
 * A draft has the target data and the user
 */

trait IdAndVersion[ID, VERSION] {
  def id: ID
  def version: VERSION
}

trait Src[DATA, ID, VERSION] {
  def data: DATA
  def id: IdAndVersion[ID, VERSION]
}

trait Draft[ID, SRC_ID, SRC_VERSION, SRC_DATA] {
  def id: ID
  def src: Src[SRC_DATA, SRC_ID, SRC_VERSION]
  def update(data: SRC_DATA): Draft[ID, SRC_ID, SRC_VERSION, SRC_DATA]
}

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

trait Drafts[ID, SRC_ID, SRC_VERSION, SRC, USER, UD <: UserDraft[ID, SRC_ID, SRC_VERSION, SRC, USER]] {

  /**
   * Creates a draft for the target data.
   */
  def create(src: SRC_ID, user: USER): UD
  def commit(d: UD, force: Boolean = false): Either[DraftError, Commit[SRC_ID, SRC_VERSION, USER]]
  def load(id: ID): Option[UD]
  def save(d: UD): Either[DraftError, ID]
}

trait DraftsWithCommitCheck[ID, SRC_ID, SRC_VERSION, SRC, USER, UD <: UserDraft[ID, SRC_ID, SRC_VERSION, SRC, USER]]
  extends Drafts[ID, SRC_ID, SRC_VERSION, SRC, USER, UD] {

  /**
   * Load commits that have used the same srcId
   * @return
   */
  def loadCommits(idAndVersion: IdAndVersion[SRC_ID, SRC_VERSION]): Seq[Commit[SRC_ID, SRC_VERSION, USER]]

  /**
   * commit the draft, create a commit and store it
   * for future checks.
   * @param d
   * @return
   */
  def commitData(d: UD): Either[DraftError, Commit[SRC_ID, SRC_VERSION, USER]]

  override def commit(d: UD, force: Boolean = false): Either[DraftError, Commit[SRC_ID, SRC_VERSION, USER]] = {
    val commits = loadCommits(d.src.id)

    if (commits.length > 0 && !force) {
      Left(CommitsWithSameSrc(commits))
    } else {
      commitData(d)
    }
  }
}
