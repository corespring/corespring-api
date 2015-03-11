package org.corespring.drafts.errors

import org.corespring.drafts.Commit

sealed abstract class DraftError(msg: String)

case class SaveDataFailed(msg: String) extends DraftError(msg)

case class CommitsWithSameSrc[ID, VERSION, USER](provenances: Seq[Commit[ID, VERSION, USER]])
  extends DraftError("There are existing data items that come from the same src")
