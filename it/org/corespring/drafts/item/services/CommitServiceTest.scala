package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ SimpleOrg, OrgAndUser, ItemCommit }
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import org.specs2.specification.{ BeforeExample, Before, BeforeAfter, Scope }
import play.api.Play
import se.radley.plugin.salat.SalatPlugin

class CommitServiceTest extends IntegrationSpecification {

  lazy val db = Db.salatDb()(Play.current)

  trait scope extends Scope {

    lazy val service = new CommitService {
      lazy val collection: MongoCollection = db("it.drafts.item_commits")
    }

    val itemId = VersionedId(ObjectId.get)

    def mkCommit(nudge: Int) = {
      ItemCommit(ObjectId.get, itemId, itemId, OrgAndUser(
        SimpleOrg(ObjectId.get, "test-org"), None),
        midday.plusMinutes(nudge))
    }

    lazy val midday = new DateTime(2015, 1, 1, 12, 0)

    def commits: Seq[ItemCommit]

    db("it.drafts.item_commits").drop()
    println(db("it.drafts.item_commits").count())
    println("--------> before...")
    commits.foreach { c =>
      println(s"save commit: $c")
      service.save(c)
    }
  }

  "CommitService" should {

    "find no commits after a date if there are no commits" in new scope {
      override def commits: Seq[ItemCommit] = Seq.empty
      service.findCommitsSince(itemId, midday) must_== Seq.empty
    }

    "find no commits after a date if there are none later than midday" in new scope {
      override def commits: Seq[ItemCommit] = Seq(
        mkCommit(-1),
        mkCommit(-2))
      service.findCommitsSince(itemId, midday) must_== Seq.empty
    }

    "find one commit after a date if there is 1 later than midday" in new scope {
      override def commits: Seq[ItemCommit] = Seq(mkCommit(1))
      service.findCommitsSince(itemId, midday).size must_== 1
    }

    "find one commit after a date if there is 1 later than midday in a collection of 2" in new scope {
      override def commits: Seq[ItemCommit] = Seq(mkCommit(1), mkCommit(-1))
      service.findCommitsSince(itemId, midday).size must_== 1
    }

    "find two commits after a date if there is 2 later than midday in a collection of 2" in new scope {
      override def commits: Seq[ItemCommit] = Seq(mkCommit(1), mkCommit(2))
      service.findCommitsSince(itemId, midday).size must_== 2
    }
  }
}
