package org.corespring.drafts

import org.corespring.drafts.errors.{ DeleteDraftFailed, CommitError, DraftError }
import org.joda.time.DateTime

import scalaz.{ Validation, Success, Failure }

case class IdVersion(id: String, version: Int)
  extends IdAndVersion[String, Int]

case class SimpleCommit(
  srcId: IdVersion,
  committedId: IdVersion,
  user: String,
  date: DateTime = DateTime.now)
  extends Commit[String, Int, String]

case class SimpleSrc(data: String, id: IdVersion) extends Src[String, String, Int]

case class SimpleDraft(id: String, src: SimpleSrc, user: String)
  extends UserDraft[String, String, Int, String, String] {
  override def update(data: String): SimpleDraft = {
    this.copy(src = src.copy(data = data))
  }
}

/**
 * A simple implementation of Drafts using mutable maps/buffers,
 * so that we can exercise the api.
 */
class SimpleStringDrafts extends DraftsWithCommitAndCreate[String, String, Int, String, String, SimpleDraft, SimpleCommit] {

  import scala.collection.mutable

  private val commits: mutable.Buffer[Commit[String, Int, String]] = mutable.Buffer.empty
  private val data: mutable.Map[String, Seq[String]] = mutable.Map.empty
  private val drafts: mutable.Buffer[SimpleDraft] = mutable.Buffer.empty

  def addData(id: String, value: String) = {
    data.put(id, Seq(value))
  }

  override def loadCommits(idAndVersion: IdAndVersion[String, Int]): Seq[Commit[String, Int, String]] = {
    commits.filter(_.srcId == idAndVersion)
  }

  override protected def saveCommit(c: SimpleCommit): Validation[CommitError, Unit] = {
    commits.append(c)
    Success()
  }

  override protected def saveDraftSrcAsNewVersion(d: SimpleDraft): Validation[DraftError, SimpleCommit] = {
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

  override protected def mkDraft(srcId: String, src: String, user: String): SimpleDraft = {
    val versions = data.get(srcId).getOrElse {
      data.put(srcId, Seq(src))
      data.get(srcId).get
    }

    val draftId = drafts.length.toString
    SimpleDraft(draftId, SimpleSrc(versions.last, IdVersion(srcId, versions.length - 1)), user)
  }

  override protected def findLatestSrc(id: String): Option[String] = {
    val versions = data.get(id).getOrElse {
      data.put(id, Seq.empty)
      data.get(id).get
    }
    versions.headOption
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

  override def load(id: String): Option[SimpleDraft] = {
    drafts.find(_.id == id)
  }

  override def save(d: SimpleDraft): Validation[DraftError, String] = {
    drafts.indexWhere(_.id == d.id) match {
      case -1 => drafts.append(d)
      case index: Int => drafts.update(index, d)
    }
    Success(d.id)
  }
}

