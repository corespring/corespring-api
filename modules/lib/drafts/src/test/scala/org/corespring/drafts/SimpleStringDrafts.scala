package org.corespring.drafts

import org.corespring.drafts.errors.DraftError
import org.joda.time.DateTime

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
class SimpleStringDrafts extends DraftsWithCommitCheck[String, String, Int, String, String, SimpleDraft] {

  import scala.collection.mutable

  private val commits: mutable.Buffer[Commit[String, Int, String]] = mutable.Buffer.empty
  private val data: mutable.Map[String, Seq[String]] = mutable.Map.empty
  private val drafts: mutable.Buffer[SimpleDraft] = mutable.Buffer.empty
  /**
   * Load commits that have used the same srcId
   * @return
   */
  override def loadCommits(idAndVersion: IdAndVersion[String, Int]): Seq[Commit[String, Int, String]] = {
    commits.filter(_.srcId == idAndVersion)
  }

  /**
   * commit the draft, create a commit and store it
   * for future checks.
   * @param d
   * @return
   */
  override def commitData(d: SimpleDraft): Either[DraftError, Commit[String, Int, String]] = {

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

    commits.append(commit)
    Right(commit)

  }

  override def load(id: String): Option[SimpleDraft] = {
    drafts.find(_.id == id)
  }

  override def save(d: SimpleDraft): Either[DraftError, String] = {
    drafts.indexWhere(_.id == d.id) match {
      case -1 => drafts.append(d)
      case index: Int => drafts.update(index, d)
    }
    Right(d.id)
  }

  /**
   * Creates a draft for the target data.
   */
  override def create(srcId: String, user: String): SimpleDraft = {
    val versions = data.get(srcId).getOrElse {
      data.put(srcId, Seq(""))
      data.get(srcId).get
    }

    val draft = SimpleDraft(
      drafts.length.toString,
      SimpleSrc(versions.last,
        IdVersion(srcId, versions.length - 1)),
      user)

    drafts.append(draft)
    draft
  }
}

