package scala.org.corespring.importing

import org.corespring.importing.Rewriter
import org.specs2.mutable.Specification

class RewriterTest extends Specification {

  "Rewrite" should {

    def unescapeCss(string: String): String = new Rewriter("""<style type="text/css">(.*?)</style>""") {
      def replacement() = s"""<style type="text/css">${group(1).replaceAll("&gt;", ">")}</style>"""
    }.rewrite(string)

    "replace all <style> &gt;" in {

      val xml = <div><style type="text/css">3 &gt; 4</style><style type="text/css">4 &gt; 3</style></div>
      println(unescapeCss(xml.toString))
      true === true
    }

  }

}
