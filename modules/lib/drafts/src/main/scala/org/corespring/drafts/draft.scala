package org.corespring.drafts.models

import org.joda.time.DateTime

trait Draft[SRC, DATA] {
  def data: DATA
  def src: Option[SRC]
  def update(d: DATA): Draft[SRC, DATA]
}

case class CommittedDraft[USER, ID, VERSION](dataId: ID, user: USER, src: DraftSrc[ID, VERSION], created: DateTime)

case class DraftSrc[ID, VERSION](id: ID, version: VERSION)

trait UserDraft[DATA, USER, ID, VERSION] extends Draft[DraftSrc[ID, VERSION], DATA] {

  def draftSrc: DraftSrc[ID, VERSION]

  def user: USER

  override final def src: Option[DraftSrc[ID, VERSION]] = Some(draftSrc)
}

