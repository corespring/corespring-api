package index

import play.core.StaticApplication

abstract class Task extends Runnable {
  val application = new StaticApplication(new java.io.File("."))
}

