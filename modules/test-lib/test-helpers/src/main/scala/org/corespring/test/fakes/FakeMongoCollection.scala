package org.corespring.test.fakes

import com.mongodb._
import com.mongodb.casbah.{ MongoCollection => CasbahMongoCollection, MongoCursor, Imports }
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture

object Fakes extends Mockito {

  /**
   * Some utilities when testing code that uses mongo collections
   */
  trait withMockCollection {

    type A2DBO = Any => DBObject

    lazy val findOneResult: DBObject = null
    lazy val findAndModifyResult: DBObject = null

    lazy val findResultSeq: Seq[DBObject] = Seq.empty

    lazy val removeResult: WriteResult = mock[WriteResult]

    lazy val findResult: MongoCursor = {
      val m = mock[MongoCursor]
      m.limit(any[Int]) returns m
      m.skip(any[Int]) returns m
      m.map(any[DBObject => Any]).answers((a: Any) => {
        val fn = a.asInstanceOf[DBObject => Any]
        findResultSeq.toIterator.map(fn)
      })
      m
    }

    def updateResult: WriteResult = mockWriteResultWithN(1)

    def mockWriteResultWithN(n: Int) = {
      val m = mock[WriteResult]
      m.getN returns n
      m
    }

    def captureRemoveQueryOnly: ArgumentCapture[DBObject] = {
      val q = capture[DBObject]
      there was one(mockCollection).remove(q, any[WriteConcern])(any[A2DBO])
      q
    }

    def captureFind: (ArgumentCapture[DBObject], ArgumentCapture[DBObject]) = {
      val q = capture[DBObject]
      val f = capture[DBObject]
      there was one(mockCollection).find(q.capture, f.capture)(any[A2DBO], any[A2DBO])
      (q, f)
    }

    def captureFindOneQueryOnly: ArgumentCapture[DBObject] = {
      val q = capture[DBObject]
      there was one(mockCollection).findOne(q.capture)(any[Any => DBObject])
      q
    }

    def captureFindOne: (ArgumentCapture[DBObject], ArgumentCapture[DBObject]) = {
      val q = capture[DBObject]
      val f = capture[DBObject]
      there was one(mockCollection).findOne(
        q.capture,
        f.capture,
        any[ReadPreference])(
          any[Any => DBObject],
          any[Any => DBObject])
      (q, f)
    }

    def captureUpdate: (ArgumentCapture[DBObject], ArgumentCapture[DBObject]) = {
      val q = capture[DBObject]
      val u = capture[DBObject]
      there was one(mockCollection).update(
        q.capture,
        u.capture,
        any[Boolean],
        any[Boolean], any[WriteConcern])(
          any[A2DBO], any[A2DBO], any[DBEncoder])
      (q, u)
    }

    def captureFindAndModify: (ArgumentCapture[DBObject], ArgumentCapture[DBObject], ArgumentCapture[DBObject], ArgumentCapture[Boolean]) = {
      val q = capture[DBObject]
      val f = capture[DBObject]
      val u = capture[DBObject]
      val n = capture[Boolean]
      there was one(mockCollection).findAndModify(
        q.capture,
        f.capture,
        any[Any],
        any[Boolean],
        u.capture,
        n.capture,
        any[Boolean])(any[A2DBO], any[A2DBO], any[A2DBO], any[A2DBO])
      (q, f, u, n)
    }

    private def nullToOption[T](o: T): Option[T] = if (o == null) {
      None
    } else {
      Some(o)
    }

    lazy val mockCollection = {
      val m = mock[CasbahMongoCollection]

      m.customEncoderFactory returns None

      m.find(any[Any], any[Any])(any[A2DBO], any[A2DBO]) returns findResult

      m.remove(any[Any], any[WriteConcern])(any[A2DBO]) returns removeResult
      m.update(
        any[Any],
        any[Any],
        any[Boolean],
        any[Boolean],
        any[WriteConcern])(
          any[A2DBO], any[A2DBO], any[DBEncoder]) returns updateResult

      m.findOne(any[Any])(any[A2DBO]) returns nullToOption[DBObject](findOneResult)

      m.findOne(
        any[Any],
        any[Any],
        any[ReadPreference])(
          any[A2DBO],
          any[A2DBO]) returns nullToOption[DBObject](findOneResult)

      m.findAndModify(
        any[Any],
        any[Any],
        any[Any],
        any[Boolean],
        any[Any],
        any[Boolean],
        any[Boolean])(
          any[A2DBO],
          any[A2DBO],
          any[A2DBO],
          any[A2DBO]) returns nullToOption(findAndModifyResult)
      m
    }
  }
}

