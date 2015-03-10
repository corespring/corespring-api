package org.corespring.drafts

import org.corespring.drafts.errors.{ DataWithSameSrc, SaveDataFailed, DraftError }
import org.corespring.drafts.models.{ CommittedDraft, DraftSrc }
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class DraftTest extends Specification {

  type StringIntSrc = DraftSrc[String, Int]

  type User = String

  case class DataAndCommit[D, C](data: D, commit: Option[C])

  class SimpleStringStore extends DraftStore[User, String, Int, String] {

    import scala.collection.mutable

    private val map: mutable.Map[String, Seq[DataAndCommit[String, CommittedDraft[User, String, Int]]]] = mutable.Map()

    override def loadDataAndVersion(id: String): (String, Int) = {
      if (!map.contains(id)) {
        map.put(id, Seq(DataAndCommit("", None)))
      }
      map.get(id).map { v => (v.last.data -> v.length) }.getOrElse {
        throw new IllegalStateException("There should be data now")
      }
    }

    override def loadCommittedDrafts(id: String): Seq[CommittedDraft[User, String, Int]] = {
      if (!map.contains(id)) {
        map.put(id, Seq(DataAndCommit("", None)))
      }
      map.get(id).map { v => v.map(_.commit).flatten }.getOrElse {
        throw new IllegalStateException("There should be data now")
      }
    }

    override def saveData(id: String, user: User, data: String, src: DraftSrc[String, Int]): Either[DraftError, CommittedDraft[User, String, Int]] = {
      if (!map.contains(id)) {
        map.put(id, Seq(DataAndCommit("", None)))
      }

      map.get(id).map { versions =>
        val dataAndCommit = DataAndCommit(data, Some(CommittedDraft(id, user, src, new DateTime())))
        val updated = versions :+ dataAndCommit
        map.put(id, updated)
        Right(dataAndCommit.commit.get)
      }.getOrElse(Left(SaveDataFailed(s"can't find item with id: $id")))
    }
  }

  "Draft" should {

    "can create a draft from a store for a user" in {
      val store = new SimpleStringStore()
      val draft = store.createDraft("1", "Ed")
      draft.src.map(_.version) === Some(1)
      draft.src.map(_.id) === Some("1")
      draft.user === "Ed"
      draft.data === ""
    }

    "can commit a draft to a store for a user" in {
      val store = new SimpleStringStore()
      val draft = store.createDraft("1", "Ed")
      val update = draft.update("update:1")
      store.commitDraft("1", update)
      val newDraft = store.createDraft("1", "Ed")
      newDraft.src === Some(DraftSrc("1", 2))
      newDraft.user === "Ed"
      newDraft.data === "update:1"
    }

    "if there is an earlier commit with the same src" should {

      val store = new SimpleStringStore()
      val edsDraft = store.createDraft("1", "Ed")
      val gwensDrafts = store.createDraft("1", "Gwen")
      val edsUpdate = edsDraft.update("draft-one:update:1")
      val gwensUpdate = gwensDrafts.update("draft-two:update:1")
      store.commitDraft("1", edsUpdate)

      "return a DraftError" in {
        store.commitDraft("1", gwensUpdate) match {
          case Left(err) => {
            err match {
              case DataWithSameSrc(commits) => {
                commits.length === 1
                commits(0).user === "Ed"
              }
              case _ => failure("wrong error")
            }
          }
          case Right(_) => failure("should fail")
        }
      }

      "commit if ignoreExistingDataWithSameSrc is true" in {
        store.commitDraft("1", gwensUpdate, ignoreExistingDataWithSameSrc = true) match {
          case Left(err) => failure("should have been successful")
          case Right(commit) => {
            commit.src.version === 1
            commit.user === "Gwen"
            store.loadDataAndVersion("1")._2 === 3
          }
        }

      }
    }

  }
}
