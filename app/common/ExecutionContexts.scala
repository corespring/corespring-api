package common

import play.api.libs.concurrent.Akka
import play.api.Play.current

/**
 * A reference to all {@link ExecutionContext}s to be made available to the application.
 *
 * See http://www.playframework.com/documentation/2.2.x/ThreadPools for more information.
 */
object ExecutionContexts {

  lazy val reportGeneration = Akka.system.dispatchers.lookup("akka.report-generator.generate-report")

}
