package org.corespring.container.client.controllers.apps

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class SourcePathsTest extends Specification {

  "js" should {

    "build NgSourcePaths" in {
      SourcePaths.js("prefix/", Json.obj(
        "src" -> Json.arr("a.js"),
        "dest" -> "dest.js",
        "libs" -> Json.arr("lib.js"),
        "ngModules" -> Json.arr("ng-module"))) must_==
        NgSourcePaths(Seq("prefix/a.js"), "prefix/dest.js", Seq("prefix/lib.js"), Seq("ng-module"))
    }
  }

  "css" should {

    "build CssSourcePaths" in {
      SourcePaths.css("prefix/", Json.obj(
        "src" -> Json.arr("a.css"),
        "dest" -> "dest.css",
        "libs" -> Json.arr("lib.css"))) must_==
        CssSourcePaths(Seq("prefix/a.css"), "prefix/dest.css", Seq("prefix/lib.css"))
    }
  }
}
