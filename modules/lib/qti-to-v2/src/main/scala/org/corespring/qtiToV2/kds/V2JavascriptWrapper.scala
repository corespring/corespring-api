package org.corespring.qtiToV2.kds

trait V2JavascriptWrapper {

  def wrap(js: JsResponseProcessing): String = {
    s"""exports.process = function(item, session, outcomes) {
      |  var answers = session.components;
      |
      |  function isCorrect(key) {
      |    return (outcomes && outcomes[key]) ? (outcomes[key].correctness === 'correct' || outcomes[key].correctness === 'all_correct') : false;
      |  }
      |
      |  function mapResponse(key) {
      |    return (outcomes && outcomes[key]) ? (outcomes[key].correctNum) : undefined;
      |  }
      |
      |  ${js.responseVars.map(responseVar => s"var $responseVar = answers['$responseVar'].answers;").mkString("\n|  ")}
      |
      |  ${js.vars.map{ case (name, value) => s"var $name = $value;"}.mkString("\n|  ")}
      |
      |  ${js.lines.mkString("\n|  ")}
      |
      |  return {
      |    summary: {
      |      ${js.vars.keySet.map(name => s"${name.toLowerCase}: $name").mkString(",\n|      ")}
      |    }
      |  };
      |};""".stripMargin
  }

}
