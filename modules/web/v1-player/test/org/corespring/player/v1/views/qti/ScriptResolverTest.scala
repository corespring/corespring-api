package org.corespring.player.v1.views.qti

import org.specs2.mutable.Specification
import org.corespring.player.v1.views.qti.models.{QtiJsAsset, QtiAssetsConfig}

class ScriptResolverTest extends Specification {


  "script resolver" should {
    "work with local paths" in {
      val config = QtiAssetsConfig(Seq(QtiJsAsset("ChoiceInteraction", localDependents = Seq("lib"))))
      def resolver = new ScriptResolver("path", ".js", config)
      resolver.getLocalPaths("ChoiceInteraction") === Seq("path/choiceInteraction.js", "path/lib.js")
    }

    "work with remote paths" in {
      val config = QtiAssetsConfig(Seq(QtiJsAsset("ChoiceInteraction", remoteDependents = Seq("http://some.com/app.js"))))
      def resolver = new ScriptResolver("path", ".js", config)
      resolver.getRemotePaths("ChoiceInteraction") === Seq("http://some.com/app.js")
    }
  }

}
