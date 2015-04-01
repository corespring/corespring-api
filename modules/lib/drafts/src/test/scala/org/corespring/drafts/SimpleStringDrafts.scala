package org.corespring.drafts

import org.corespring.drafts.errors.{ DeleteDraftFailed, CommitError, DraftError }
import org.joda.time.DateTime

import scalaz.{ Validation, Success, Failure }

case class IdVersion(id: String, version: Int)

case class SimpleCommit(
  srcId: IdVersion,
  committedId: IdVersion,
  user: String,
  date: DateTime = DateTime.now)
  extends Commit[IdVersion, String]

case class SimpleSrc(data: String, id: IdVersion) extends Src[IdVersion, String] {
  override protected def dataWithVid: HasVid[IdVersion] = new HasVid[IdVersion] {
    override def id: IdVersion = SimpleSrc.this.id
  }
}

case class SimpleDraft(id: String, src: SimpleSrc, user: String)
  extends UserDraft[String, IdVersion, String, String] {
  override def update(data: String): SimpleDraft = {
    this.copy(src = src.copy(data = data))
  }

  override def created: DateTime = DateTime.now

  override def expires: DateTime = DateTime.now.plusDays(1)
}

/**
 * A simple implementation of Drafts using mutable maps/buffers,
 * so that we can exercise the api.
 */

class SimpleStringDrafts extends DraftsWithCommitAndCreate[String, IdVersion, String, String, SimpleDraft, SimpleCommit] {

  import scala.collection.mutable

  private val commits: mutable.Buffer[SimpleCommit] = mutable.Buffer.empty
  private val data: mutable.Map[String, Seq[String]] = mutable.Map.empty
  private val drafts: mutable.Buffer[SimpleDraft] = mutable.Buffer.empty

  def addData(id: String, value: String) = {
    data.put(id, Seq(value))
  }

  override protected def loadCommitsNotByDraft(draftId: String, idAndVersion: IdVersion): Seq[SimpleCommit] = {
    commits.filter(_.srcId == idAndVersion)
  }

  override protected def saveCommit(c: SimpleCommit): Validation[CommitError, Unit] = {
    commits.append(c)
    Success()
  }

  override protected def saveDraftBackToSrc(d: SimpleDraft): Validation[DraftError, SimpleCommit] = {
    val versions = data.get(d.id).getOrElse {
      data.put(d.id, Seq.empty)
      data.get(d.id).get
    }

    val update = versions :+ d.src.data
    data.put(d.id, update)
    val newVersion = update.length
    val srcIdVersion = IdVersion(d.src.id.id, d.src.id.version)
    val commit = SimpleCommit(
      srcIdVersion,
      srcIdVersion.copy(version = newVersion),
      d.user)
    Success(commit)
  }

  override protected def mkDraft(srcId: IdVersion, src: String, user: String): Validation[DraftError, SimpleDraft] = {
    val versions = data.get(srcId.id).getOrElse {
      data.put(srcId.id, Seq(src))
      data.get(srcId.id).get
    }

    val draftId = drafts.length.toString
    Success(SimpleDraft(draftId, SimpleSrc(versions.last, IdVersion(srcId.id, versions.length - 1)), user))
  }

  override protected def findLatestSrc(id: IdVersion): Option[String] = {
    val versions = data.get(id.id).getOrElse {
      data.put(id.id, Seq.empty)
      data.get(id.id).get
    }
    versions.headOption
  }

  override protected def userCanCreateDraft(id: IdVersion, user: String): Boolean = {
    true
  }

  override protected def deleteDraft(d: SimpleDraft): Validation[DraftError, Unit] = {
    val index = drafts.indexWhere(_.id == d.id)
    if (index == -1) {
      Failure(DeleteDraftFailed(d.id))
    } else {
      drafts.remove(index)
      Success()
    }
  }

  override def load(user: String)(id: String): Option[SimpleDraft] = {
    drafts.find(_.id == id)
  }

  override def save(user: String)(d: SimpleDraft): Validation[DraftError, String] = {
    drafts.indexWhere(_.id == d.id) match {
      case -1 => drafts.append(d)
      case index: Int => drafts.update(index, d)
    }
    Success(d.id)
  }

  override protected def updateDraftSrcId(d: SimpleDraft, newSrcId: IdVersion): Validation[DraftError, Unit] = Success(Unit)
}

