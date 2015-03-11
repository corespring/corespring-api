package org.corespring.drafts

import org.corespring.drafts.errors.CommitsWithSameSrc
import org.specs2.mutable.Specification

class DraftTest extends Specification {

  type User = String

  "SimpleDraft" should {

    "when creating a draft" should {
      val drafts = new SimpleStringDrafts()
      val draft = drafts.create("1", "Ed")
      "set the data" in draft.src.data === ""
      "set the user" in draft.user === "Ed"
      "set the id" in draft.id === "0"
      "allows you to load by id" in drafts.load("0") === Some(draft)
    }

    "when committing a draft" should {
      val drafts = new SimpleStringDrafts()
      val draft = drafts.create("1", "Ed")
      val update = draft.update("update:1")
      val result = drafts.commit(update)
      "result is ok" in result.isRight
      "commit data is set" in {
        val commit = result.right.get
        commit.srcId.id === "1"
        commit.srcId.version === 0
        commit.committedId.id === "1"
        commit.committedId.version === 1
      }
    }

    "when ed and gwen both commit from the same src" should {
      val drafts = new SimpleStringDrafts()
      val eds = drafts.create("1", "Ed")
      val gwens = drafts.create("1", "Gwen")
      val edsUpdate = eds.update("ed's update")
      val gwensUpdate = eds.update("gwen's update")
      val edsCommit = drafts.commit(edsUpdate)
      val gwensCommit = drafts.commit(gwensUpdate)
      "eds commit is ok as it was first" in edsCommit.isRight === true
      "gwens commit is not ok" in gwensCommit.isLeft === true
      "for gwen's commit, ed's commit is listed as a commit with the same src" in {
        gwensCommit match {
          case Left(CommitsWithSameSrc(commits)) => {
            commits.length === 1
            val c = commits(0)
            c.user === "Ed"
          }
          case _ => failure("should have got a CommitsWithSameSrc error")
        }
      }
    }
  }
}
