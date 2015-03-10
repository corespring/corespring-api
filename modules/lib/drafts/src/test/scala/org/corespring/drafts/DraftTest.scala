package org.corespring.drafts

import org.specs2.mutable.Specification

class DraftTest extends Specification {

  type StringIntSrc = DraftSrc[String, Int]

  class SimpleStringStore extends DraftStore[String, Int, String] {

    import scala.collection.mutable

    private val map: mutable.Map[String, Seq[Draft[StringIntSrc, String]]] = mutable.Map()

    override def loadDataAndVersion(id: String): (String, Int) = {
      if (!map.contains(id)) {
        map.put(id, Seq(InitialDraft("")))
      }
      map.get(id).map { v => (v.last.data -> v.length) }.getOrElse {
        throw new IllegalStateException("There should be data now")
      }
    }

    override def mkInitialDraft: InitialDraft[String, Int, String] = InitialDraft("")

    override def loadEarlierDrafts(id: String): Seq[Draft[StringIntSrc, String]] = {
      if (!map.contains(id)) {
        map.put(id, Seq(InitialDraft("")))
      }

      map.get(id).getOrElse {
        throw new IllegalStateException("There should be data now")
      }
    }

    override def saveData(id: String, data: String, src: StringIntSrc): Either[DraftError, Int] = {
      if (!map.contains(id)) {
        map.put(id, Seq(InitialDraft("")))
      }

      map.get(id).map { versions =>
        val updated = versions :+ UserDraft[String, Int, String](data, Some(src))
        map.put(id, updated)
        Right(updated.length)
      }.getOrElse(Left(SaveDataFailed(s"can't find item with id: $id")))
    }
  }

  "Draft" should {

    "can create a draft from a store" in {
      val store = new SimpleStringStore()
      val draft = store.createDraft("1")
      draft.src.map(_.version) === Some(1)
      draft.src.map(_.id) === Some("1")
      draft.data === ""
    }

    "can commit a draft to a store" in {
      val store = new SimpleStringStore()
      val draft = store.createDraft("1")
      val update: Draft[StringIntSrc, String] = draft.update("update:1")
      store.commitDraft("1", update.data, update.src.get)
      val newDraft = store.createDraft("1")
      newDraft.src === Some(DraftSrc("1", 2))
      newDraft.data === "update:1"
    }

    "if there is an earlier commit with the same src, return a DraftError" in {
      val store = new SimpleStringStore()
      val draftOne = store.createDraft("1")
      val updateOne: Draft[StringIntSrc, String] = draftOne.update("draft-one:update:1")
      val updateTwo: Draft[StringIntSrc, String] = draftOne.update("draft-one:update:2")
      store.commitDraft("1", updateOne.data, updateOne.src.get)

      store.commitDraft("1", updateTwo.data, updateTwo.src.get) match {
        case Left(err) => {
          err match {
            case EarlierDraftsWithSameSrc(drafts) => {
              drafts.length === 1
              drafts(0).data === "draft-one:update:1"
            }
            case _ => failure("wrong error")
          }
        }
        case Right(_) => failure("should fail")
      }
    }

    "ignore earlier drafts with same src error" in { true === false }.pendingUntilFixed

    "associate a draft with a user" in { true === false }.pendingUntilFixed

  }
}
