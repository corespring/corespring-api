angular.module('qti.directives').directive('simplechoice', function (QtiUtils) {

  return {
    restrict: 'ACE',
    replace: true,
    scope: true,
    require: '^choiceinteraction',

    compile: function (tElement, tAttrs, transclude) {

      var feedbackInlineRegex = /(<span.*?feedbackInline.*?<\/.*?>)/gi;


      /**
       * Note - in choiceInteraction.compile we wrap this in varying number of divs - so we need to find the choiceInteraction ancestor node.
       */
      var choiceInteractionElem = tElement.parent();
      var i = 0;
      while (choiceInteractionElem.attr('responseidentifier') == undefined) {
        choiceInteractionElem = choiceInteractionElem.parent();
        if (i++ > 10) throw new Error("Parent choice interaction not found");
      }


      var maxChoices = choiceInteractionElem.attr('maxChoices');
      var isHorizontal = choiceInteractionElem.attr('orientation') == 'horizontal';
      var inputType = maxChoices == 1 ? 'radio' : 'checkbox';
      var modelName = inputType == 'radio' ? 'radioGroupChosenItem' : 'checkBoxItemSelected';


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
          '  <div class="choice-content"> ' + nodeWithFeedbackRemoved + '</div>',
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

        localScope.$watch('controller.scope.chosenItem', function (newValue) {

          if (!newValue) {
            return;
          }

          if (inputType == 'radio') {
            localScope.radioGroupChosenItem = newValue;
          } else {
            localScope.checkBoxItemSelected = newValue.indexOf(localScope.value) != -1;
          }
        });

        var isSelected = function () {
          var responseValue = QtiUtils.getResponseValue(responseIdentifier, localScope.responses, "");
          return QtiUtils.compare(localScope.value, responseValue);
        };

        var isOurResponseCorrect = function (correctResponse) {
          return QtiUtils.compare(localScope.value, correctResponse)
        };

        var applyCorrectResponseStyle = function () {
          clear('correct-response', 'correct-response-horizontal');

          element.toggleClass(isHorizontal ? 'correct-response-horizontal' : 'correct-response');
        };

        var clear = function () {
          for (var i = 0; i < arguments.length; i++) {
            element.removeClass(arguments[i]);
          }
        };

        var tidyUp = function () {
          element
            .removeClass('incorrect-response')
            .removeClass('correct-response')
            .removeClass('received-response')
            .removeClass('incorrect-response-horizontal')
            .removeClass('correct-response-horizontal')
            .removeClass('received-response-horizontal');
        };


        localScope.$on('resetUI', function (event) {
          tidyUp();
        });

        /**
         * Programmatically show the user selection for the finished item
         */
        localScope.$on('highlightUserResponses', function (event) {
          var responseValue = QtiUtils.getResponseValue(responseIdentifier, localScope.itemSession.responses, "");

          if (typeof(responseValue) !== "string") {
            for (var i = 0; i < responseValue.length; i++) {
              choiceInteractionController.scope.setChosenItem(responseValue[i], true);
            }
          } else {
            choiceInteractionController.scope.setChosenItem(responseValue, true);
          }
        });

        localScope.$on('unsetSelection', function (event) {
          localScope.chosenItem = [];
        });


        // watch the status of the item, update the css if this is the chosen response
        // and if it is correct or not
        localScope.$watch('itemSession.sessionData.correctResponses', function (responses) {
          if (!responses) return;

          var correctResponse = QtiUtils.getResponseValue(responseIdentifier, responses, "");

          var isCorrect = isOurResponseCorrect(correctResponse);

          tidyUp();

          if (isCorrect && localScope.highlightCorrectResponse()) {
            applyCorrectResponseStyle();
          }
          if (isSelected() && localScope.highlightUserResponse()) {
            var className;

            if (responses.length == 0)
              className = "received-response"; // no information about correctness
            else
              className = isCorrect ? 'correct-response' : 'incorrect-response';

            element.addClass(isHorizontal ? (className + "-horizontal") : className);
          }
        });
      };
    }
  };
});
