

import scalaz._
import scalaz.Scalaz._
val s = true.success[String]
val f = false.failure[Boolean].leftMap{ b => "No"}
println(s)
println(f)