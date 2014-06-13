type PF = PartialFunction[String, Option[String]]

val a : PartialFunction[String,Option[String]] =  {case (s:String) => Some(s + "a")}
val b : PartialFunction[String,Option[String]] =  {case (s:String) => Some(s + "b")}
val c : PartialFunction[String,Option[String]] =  {case (s:String) => Some(s + "c")}
val d : PartialFunction[String,Option[String]] =  {case (s:String) => Some(s + "d")}
val y : PartialFunction[String,Option[String]] = {case _ => None}

def aa(s:String) = Some(s + "a")
def bb(s:String) = Some(s + "b")
def cc(s:String) = None

def foldFn(a:PF,b:PF) = a.orElse(b)

Seq(a).fold[PF](y)(foldFn)("hello")
Seq(a,b).fold[PF](y)(foldFn)("hello")
Seq(y).fold[PF](a)(foldFn)("hello")

(a orElse b orElse c)("hello")
(y orElse b orElse c)("hello")

var r = None

Seq(1,3,4).foreach{ n => if(r.isDefined) r else Some(n)}


Seq(aa _, bb _).foldLeft[Option[String]](None){(acc, fn) =>

  if(acc.isDefined){
    acc
  } else {
    fn("s")
  }
}

Seq(cc _, cc _, bb _).foldLeft[Option[String]](None){(acc, fn) =>

  if(acc.isDefined){
    acc
  } else {
    fn("s")
  }
}
