package index

import play.api.Play
import play.api.test.FakeApplication

object IndexerRunner {

  def run = {
    Play.start(FakeApplication())
    Indexer.initialize()
  }
}
