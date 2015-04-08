package org.corespring.drafts.item

import org.corespring.drafts.errors.{ CommitsAfterDraft, CommitError }
import org.corespring.drafts.item.models.{ ItemCommit, ItemDraft }
import org.corespring.drafts.item.services.CommitService

import scalaz.{ Failure, Success, Validation }

trait CommitCheck {

  def commitService: CommitService

  /** Check whether this draft may be committed */
  def canCommit(d: ItemDraft): Validation[CommitError, Unit] = {
    commitsAfterDraft(d) match {
      case Nil => Success()
      case head :: xs => {
        Failure(CommitsAfterDraft(head +: xs))
      }
    }
  }

  def commitsAfterDraft(d: ItemDraft): Seq[ItemCommit] = {
    commitService.findCommitsSince(d.src.data.id, d.committed.getOrElse(d.created))
  }

}
