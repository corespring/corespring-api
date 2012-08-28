import org.specs2.mutable.Specification

/**
 * Base class for tests
 *
 */
abstract class BaseTest extends Specification {
  PlaySingleton.start()

  // from standard fixture data
  val token = "34dj45a769j4e1c0h4wb"
}
