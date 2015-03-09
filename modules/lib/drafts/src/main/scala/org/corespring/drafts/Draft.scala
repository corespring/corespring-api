package org.corespring.drafts

trait DraftStore[ID, VERSION, DATA] {
  def createDraft(id: ID): Draft[DraftSrc[ID, VERSION], DATA]

  def commitDraft(id: ID, draft: Draft[DraftSrc[ID, VERSION], DATA]): Either[String, String]
}

trait SimpleStore[ID, VERSION, DATA] extends DraftStore[ID, VERSION, DATA] {

  type DraftAndSrc = Draft[DraftSrc[ID, VERSION], DATA]

  def loadDataAndVersion(id: ID): (DATA, VERSION)

  def saveData(id: ID, data: DATA, src: (ID, VERSION)): Either[String, VERSION]

  def mkInitialDraft: InitialDraft[ID, VERSION, DATA]

  def loadEarlierDrafts(id: ID): Seq[Draft[DraftSrc[ID, VERSION], DATA]]

  override def createDraft(id: ID): DraftAndSrc = {
    val (data, version) = loadDataAndVersion(id)
    UserDraft[ID, VERSION, DATA](data, Some(DraftSrc[ID, VERSION](id, version)))
  }

  override def commitDraft(id: ID, draft: DraftAndSrc): Either[String, VERSION] = {

    def getEarlierDraftsWithSameSrc(versions: Seq[DraftAndSrc]) = {

      def previousDraftWithSameSrc(pd: DraftAndSrc): Boolean = {
        pd.src.isDefined && pd.src == draft.src
      }
      versions.filter(previousDraftWithSameSrc)
    }

    val versions = loadEarlierDrafts(id)
    val earlierDrafts = getEarlierDraftsWithSameSrc(versions)
    println(s"earlier: $earlierDrafts")

    if (earlierDrafts.length > 0) {
      Left("Not Ok")
    } else {
      saveData(id, draft.data)
    }
  }

}

case class DraftSrc[ID, VERSION](id: ID, version: VERSION)

trait Draft[SRC, DATA] {
  def data: DATA
  def src: Option[SRC]
  def update(d: DATA): Draft[SRC, DATA]
}

case class UserDraft[ID, VERSION, DATA](data: DATA, src: Option[DraftSrc[ID, VERSION]])
  extends Draft[DraftSrc[ID, VERSION], DATA] {
  override def update(d: DATA): Draft[DraftSrc[ID, VERSION], DATA] = this.copy(data = d)
}

case class InitialDraft[ID, VERSION, DATA](data: DATA)
  extends Draft[DraftSrc[ID, VERSION], DATA] {
  override def src: Option[DraftSrc[ID, VERSION]] = None

  override def update(d: DATA): Draft[DraftSrc[ID, VERSION], DATA] = this.copy(data = d)
}
