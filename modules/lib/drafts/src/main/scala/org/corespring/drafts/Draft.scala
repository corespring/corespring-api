package org.corespring.drafts

trait DraftStore[ID, VERSION, DATA] {
  type DraftAndSrc = Draft[DraftSrc[ID, VERSION], DATA]

  def loadDataAndVersion(id: ID): (DATA, VERSION)

  def saveData(id: ID, data: DATA, src: DraftSrc[ID, VERSION]): Either[DraftError, VERSION]

  def mkInitialDraft: InitialDraft[ID, VERSION, DATA]

  def loadEarlierDrafts(id: ID): Seq[DraftAndSrc]

  def createDraft(id: ID): DraftAndSrc = {
    val (data, version) = loadDataAndVersion(id)
    UserDraft[ID, VERSION, DATA](data, Some(DraftSrc[ID, VERSION](id, version)))
  }

  def commitDraft(id: ID, data: DATA, src: DraftSrc[ID, VERSION], ignoreEarlierDraftsWithSameSrc: Boolean = false): Either[DraftError, VERSION] = {

    def getEarlierDraftsWithSameSrc(versions: Seq[DraftAndSrc]) = {
      versions.filter(pd => pd.src.isDefined && pd.src.get == src)
    }

    val earlierDrafts = loadEarlierDrafts(id)
    val earlierDraftsWithSameSrc = getEarlierDraftsWithSameSrc(earlierDrafts)
    println(s"earlier: $earlierDrafts")
    println(s"earlier with same src: $earlierDraftsWithSameSrc")

    if (earlierDraftsWithSameSrc.length > 0 && !ignoreEarlierDraftsWithSameSrc) {
      Left(EarlierDraftsWithSameSrc(earlierDraftsWithSameSrc))
    } else {
      saveData(id, data, src)
    }
  }
}

sealed abstract class DraftError(msg: String)

case class SaveDataFailed(msg: String) extends DraftError(msg)
case class EarlierDraftsWithSameSrc[ID, VERSION, DATA](drafts: Seq[Draft[DraftSrc[ID, VERSION], DATA]])
  extends DraftError("There are earlier drafts with the same src")

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
