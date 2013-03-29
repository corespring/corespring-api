qtiDirectives.directive('simplechoice', function (QtiUtils) {

  return {
    restrict: 'ACE',
    replace: true,
    scope: true,
    require: '^choiceinteraction',

    compile: function (tElement, tAttrs, transclude) {

      var feedbackInlineRegex = /(<span.*?feedbackInline.*?<\/.*?>)/gi;
      var identifier = tAttrs['identifier'];

      var choiceInteractionElem = tElement.parent();
      var i = 0;
      while (choiceInteractionElem.attr('responseidentifier') == undefined) {
        choiceInteractionElem = choiceInteractionElem.parent();
        if (i++ > 10) throw new Error("Parent choice interaction not found");
      }


      var maxChoices = choiceInteractionElem.attr('maxChoices');
      var isHorizontal = choiceInteractionElem.attr('orientation') == 'horizontal';
      var inputType = maxChoices == 1 ? 'radio' : 'checkbox';
      var modelName = 'chosen'

      var nodeWithFeedbackRemoved = tElement.html().replace(feedbackInlineRegex, "");
      var responseIdentifier = choiceInteractionElem.attr('responseidentifier');

      var divs = isHorizontal ? [
        '<div class="simple-choice-inner-horizontal" ng-class="{noResponse: noResponse}">',
        '   <div class="choice-content-horizontal" ng-class="{noResponse: noResponse}"> ' + nodeWithFeedbackRemoved + '</div>',
        '   <div ng-class="{noResponse: noResponse}"><input type="' + inputType + '" ng-click="onClick()" ng-disabled="formSubmitted" ng-model="' + modelName + '" value="{{value}}"></input></div>',
        '</div>'
      ]
        :
        ['<div class="simple-choice-inner">',
          '  <div class="choiceInput">',
          '    <input type="' + inputType + '" ng-click="onClick()" ng-disabled="formSubmitted"  ng-model="' + modelName + '" value="{{value}}"></input></div>',
          '  <div class="choice-content"><span class="{{resultClass}}">{{percentage}}% </span>' + nodeWithFeedbackRemoved + '</div>',
          '</div>'
        ];

      var template = divs.join("\n");


      // now can modify DOM
      tElement.html(template);

      // return link function
      return function (localScope, element, attrs, choiceInteractionController) {
        localScope.disabled = false;
        localScope.value = attrs.identifier;
        localScope.controller = choiceInteractionController;

        localScope.$watch('aggregate', function (aggregate) {
          if (!aggregate) return;
          var agg = aggregate[responseIdentifier];
          if (!agg) return;

          var total = agg.totalDistribution;
          localScope.numResponse = aggregate[responseIdentifier].choices[identifier] || 0;
          localScope.percentage = (localScope.numResponse / total * 100).toFixed(0);
          var isCorrect = agg.correctAnswers.indexOf(localScope.value) >= 0;
          localScope.resultClass = isCorrect ? "correct" : "incorrect";
          if (inputType == 'radio') {
            localScope.chosen = agg.correctAnswers[0];
          } else {
            localScope.chosen = agg.correctAnswers.indexOf(localScope.value) >= 0;
          }
        });

      };
    }
  };
});
