import play.api.Play
import play.api.test.FakeApplication

object PlaySingleton {
  def start() {
    this.synchronized {
      Play.maybeApplication match {
        case Some(fakeApp) =>
        case None => Play.start(FakeApplication())
      }
    }
  }

  def stop() {
    this.synchronized {
      Play.maybeApplication match {
        case Some(fakeApp) => {
          Global.onStop(fakeApp)
          Play.stop()
        }
        case None =>
      }
    }
  }
}