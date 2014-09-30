import sbt.Keys._
import sbt._

object Commands {

  def runJsTests(u:Unit) : Unit = {
    import scala.sys.process._
    println("!! JS-Tests---------------------------------------------")
    val logger = ProcessLogger(line => println(line), line => println(line))
    val exitCode: Int = "./js-test-runner" ! logger
    println("!! Exit code: " + exitCode)
    println("!! End JS-Tests---------------------------------------------")
    if (exitCode != 0) sys.error("error")
    u
  }

}