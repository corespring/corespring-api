package org.corespring.log

import org.specs2.mutable.Specification
import org.corespring.clientlogging.{MessageType, ClientLogEntry}
import java.util.Date


class ClientLoggingTest extends Specification{

  "client log entry" should {
    "have correct output" in {
      val end = new Date()
      val logEntry = new ClientLogEntry("default", end)
      val message = "blah"
      val messageType = MessageType.Info
      val extraargs = Seq()
      val output = logEntry.toString("blah",MessageType.Info)
      output === Seq(
        s"\n***Client Log Entry***",
        s"${messageType}: ${message}\n",
        s"${extraargs.mkString("\n")}",
        s"***End ${end.toString}***"
      ).mkString("\n")
    }
  }
}
