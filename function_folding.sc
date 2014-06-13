
val a : (String => String) = (a : String) => { a + "!"}
val b : (String => String) = (a : String) => { a + "?"}

val c : (String => Option[String]) = (a:String) => None
val d : (String => Option[String]) = (a:String) => Some("!!")


val seq = Seq(a,b)

val folded = seq.foldRight("input"){ (fn1, fn2) =>

  fn()
  (fn1).andThen(fn2)

}
println("............")
folded("hello")
