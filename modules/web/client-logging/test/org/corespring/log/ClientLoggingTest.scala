package org.corespring.log

import org.specs2.mutable.Specification


class ClientLoggingTest extends Specification{

  "client log entry" should {
    "have correct output" in {
      val logEntry = new ClientLogEntry("default")
      val message = "blah"
      val messageType = MessageType.Info
      val extraargs = Seq()
      val output = logEntry.toString("blah",MessageType.Info)
      output === Seq(
        s"\n***Client Log Entry***",
        s"${messageType}: ${message}\n",
        s"${extraargs.mkString("\n")}",
        s"***End ${new java.util.Date().toString()}***"
      ).mkString("\n")
    }
  }
}
