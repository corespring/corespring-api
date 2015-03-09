package org.corespring.drafts

import org.specs2.mutable.Specification

class DraftTest extends Specification {

  class SimpleStringStore extends SimpleStore[String, Int, String] {

    import scala.collection.mutable

    private val map: mutable.Map[String, Seq[Draft[DraftSrc[String, Int], String]]] = mutable.Map()

    override def loadDataAndVersion(id: String): (String, Int) = {
      if (!map.contains(id)) {
        map.put(id, Seq(InitialDraft("")))
      }
      map.get(id).map { v => (v.last.data -> v.length) }.getOrElse {
        throw new IllegalStateException("There should be data now")
      }
    }

    override def mkInitialDraft: InitialDraft[String, Int, String] = InitialDraft("")

    override def loadEarlierDrafts(id: String): Seq[Draft[DraftSrc[String, Int], String]] = {
      if (!map.contains(id)) {
        map.put(id, Seq(InitialDraft("")))
      }

      map.get(id).getOrElse {
        throw new IllegalStateException("There should be data now")
      }
    }

    override def saveData(id: String, data: String, src: (String, Int)): Either[String, Int] = {

    }
    /*override def createDraft(id: String): SimpleStringDraft = {
      val versions = map.get(id).getOrElse{
        val initialData : Draft[DraftSrc[String,Int],String] = InitialDraft("")
        val versions = Seq(initialData)
        map.put(id,versions)
        versions
      }
      require(versions.length > 0)

      UserDraft[String,Int,String](versions.last.data, Some(DraftSrc[String,Int](id, versions.length)))
    }

    override def commitDraft(id: String, draft: SimpleStringDraft): Either[String, String] = {

      def getEarlierDraftsWithSameSrc(versions:Seq[SimpleStringDraft]) = {

        def previousDraftWithSameSrc(pd:SimpleStringDraft) : Boolean = {
          pd.src.isDefined && pd.src == draft.src
        }
        versions.filter(previousDraftWithSameSrc)
      }

      map.get(id).map{ versions =>
        val earlierDrafts = getEarlierDraftsWithSameSrc(versions)
        println(s"earlier: $earlierDrafts")
        if(earlierDrafts.length > 0){
          Left("Not Ok")
        } else {
          map.put(id, versions :+ draft)
          Right("Ok")
        }
      }.getOrElse{
        map.put(id, Seq(draft))
        Seq(draft)
        Right("OK")
      }
    }*/
  }

  "Draft" should {

    val store = new SimpleStringStore()

    "can create a draft from a store" in {
      val draft = store.createDraft("1")
      draft.src.map(_.version) === Some(1)
      draft.src.map(_.id) === Some("1")
      draft.data === ""
    }

    "can commit a draft to a store" in {
      val draft = store.createDraft("1")
      val update: Draft[StringIntSrc, String] = draft.update("update:1")
      store.commitDraft("1", update)
      val newDraft = store.createDraft("1")
      newDraft.src === Some(DraftSrc("1", 2))
      newDraft.data === "update:1"
    }

    "if there is an earlier commit with the same src, warn" in {
      val draftOne = store.createDraft("1")
      println(draftOne.src)
      val updateOne: Draft[StringIntSrc, String] = draftOne.update("draft-one:update:1")
      println(updateOne.src)
      val updateTwo: Draft[StringIntSrc, String] = draftOne.update("draft-one:update:2")
      println(updateTwo.src)

      store.commitDraft("1", updateOne)
      val result = store.commitDraft("1", updateTwo)
      println(s"result: $result")
      result match {
        case Left(_) => success
        case Right(_) => failure
      }
    }

  }
}
