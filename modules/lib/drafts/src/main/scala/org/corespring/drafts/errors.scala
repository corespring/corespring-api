package org.corespring.drafts.errors

import org.corespring.drafts.models.CommittedDraft

sealed abstract class DraftError(msg: String)

case class SaveDataFailed(msg: String) extends DraftError(msg)

case class DataWithSameSrc[DATA, USER, ID, VERSION](provenances: Seq[CommittedDraft[USER, ID, VERSION]])
  extends DraftError("There are existing data items that come from the same src")
