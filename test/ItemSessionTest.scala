import models.{ItemResponse, ItemSession}
import org.joda.time.DateTime
import org.specs2.execute.{Failure, Pending}
import org.specs2.internal.scalaz.Success
import play.api.test.FakeApplication
import org.specs2.specification.Fragments
import org.specs2.mutable.Specification

/**
 * Tests the ItemSession model
 */
class ItemSessionTest extends Specification {

  PlaySingleton.start()
  testSaveAndDelete

  def testSaveAndDelete: Fragments = {

    val testSession = ItemSession()

    "ItemSession" should {
      "be saveable" in {
        ItemSession.save(testSession)
        ItemSession.findOneById(testSession.id) match {
          case Some(result) => success
          case _ => failure
        }
      }

      "be deletable" in {
        ItemSession.remove(testSession)
        ItemSession.findOneById(testSession.id) match {
          case Some(result) => failure
          case _ => success
        }
      }
    }

  }
}
