package org.corespring.drafts

import org.corespring.drafts.errors.{ DataWithSameSrc, DraftError }
import org.joda.time.DateTime

/**
 * A draft has the target data and the user
 */
trait Draft[ID, SRC_ID, SRC] {
  def id: ID
  def src: SRC
  def srcId: SRC_ID
}

trait UserDraft[ID, SRC_ID, SRC, USER] extends Draft[ID, SRC_ID, SRC] {
  def user: USER
}

trait Commit[SRC_ID, USER] {
  def srcId: SRC_ID
  def committedId: SRC_ID
  def date: DateTime
  def user: USER
}

trait Drafts[ID, SRC_ID, SRC, USER] {

  type UD = UserDraft[ID, SRC_ID, SRC, USER]

  /**
   * Creates a draft for the target data.
   */
  def create(src: SRC, user: USER): UD
  def createFromId(id: SRC_ID, user: USER): UD
  def save(d: UD): Either[String, ID]
  def commit(d: UD, force: Boolean = false): Either[String, Commit[SRC_ID, USER]]
  def load(id: ID): Option[UD]
}

trait DraftWithCommitCheck[ID, SRC_ID, SRC, USER] extends Drafts[ID, SRC_ID, SRC, USER] {

  /**
   * Load commits that have used the same srcId
   * @param srcId
   * @return
   */
  def loadCommits(srcId: SRC_ID): Seq[Commit[SRC_ID, USER]]

  /**
   * commit the draft, create a commit and store it
   * for future checks.
   * @param d
   * @return
   */
  def commitData(d: UD): Either[DraftError, Commit[SRC_ID, USER]]

  override def commit(d: UD, force: Boolean = false): Either[DraftError, Commit[SRC_ID, USER]] = {
    val commits = loadCommits(d.srcId)

    if (commits.length > 0 && !force) {
      Left(DataWithSameSrc(commits))
    } else {
      commitData(d)
    }
  }
}
