package org.corespring.drafts

import org.corespring.drafts.errors.CommitsWithSameSrc
import org.specs2.mutable.Specification

import scalaz.Failure

class DraftTest extends Specification {

  type User = String

  "SimpleDraft" should {

    "when creating a draft" should {
      val drafts = new SimpleStringDrafts()
      drafts.addData("1", "initial data")
      val draft = drafts.create("1", "Ed")
      "set the data" in draft.get.src.data === "initial data"
      "set the user" in draft.get.user === "Ed"
      "set the id" in draft.get.id === "0"
      "allows you to load by id" in drafts.load("Ed")("0") === draft
    }

    "when committing a draft" should {
      val drafts = new SimpleStringDrafts()
      drafts.addData("1", "initial data")
      val draft = drafts.create("1", "Ed").get
      val update = draft.update("update:1")
      val result = drafts.commit("Ed")(update)
      "result is ok" in result.isSuccess
      "commit data is set" in {
        val commit = result.toOption.get
        commit.srcId.id === "1"
        commit.srcId.version === 0
        commit.committedId.id === "1"
        commit.committedId.version === 1
      }
    }

    "when ed and gwen both commit from the same src" should {
      val drafts = new SimpleStringDrafts()
      drafts.addData("1", "initial data")

      val eds = drafts.create("1", "Ed").get
      val gwens = drafts.create("1", "Gwen").get
      val edsUpdate = eds.update("ed's update")
      val gwensUpdate = eds.update("gwen's update")
      val edsCommit = drafts.commit("Ed")(edsUpdate)
      val gwensCommit = drafts.commit("Gwen")(gwensUpdate)
      "eds commit is ok as it was first" in edsCommit.isSuccess === true
      "gwens commit is not ok" in gwensCommit.isFailure === true
      "for gwen's commit, ed's commit is listed as a commit with the same src" in {
        gwensCommit match {
          case Failure(CommitsWithSameSrc(commits)) => {
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
