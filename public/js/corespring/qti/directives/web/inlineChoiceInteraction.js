qtiDirectives.directive('inlinechoiceinteraction', function (QtiUtils) {

  var link = function ($scope, element, attrs, AssessmentItemCtrl) {
    console.log('inlinechoiceinteraction!');

    AssessmentItemCtrl.registerInteraction(element.attr('responseIdentifier'), 'inline');

    var modelToUpdate = attrs["responseidentifier"];

    $scope.$watch('choice', function (newValue) {
      AssessmentItemCtrl.setResponse(modelToUpdate, newValue);
    });

    $scope.$on('resetUI', function (event) {
      element
        .removeClass('correct-response')
        .removeClass('incorrect-response');
    });

    $scope.$on('unsetSelection', function (event) {
      $scope.choice = "";
    });

    $scope.$on('highlightUserResponses', function () {
      $scope.choice = QtiUtils.getResponseValue(modelToUpdate, $scope.itemSession.responses);
    });

    $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {

      if (!responses) return;

      var correctResponse = QtiUtils.getResponseValue(modelToUpdate, responses, "");
      var isCorrect = QtiUtils.compare($scope.choice, correctResponse);

      element
        .removeClass('correct-response')
        .removeClass('incorrect-response');

      if (isCorrect) {
        if ($scope.highlightCorrectResponse() || $scope.highlightUserResponse()) {
          element.addClass('correct-response');
        }
      } else {
        if ($scope.highlightUserResponse()) {
          element.addClass('incorrect-response');
        }
      }
    });

  };


  var getOptionsAndFeedbacks = function (element) {

    var html = element.html();

    var inlineChoiceRegex = /(<inlinechoice[\s\S]*?>[\s\S]*?<\/inlinechoice>)/gm;
    var feedbackRegex = /(<feedbackinline[\s\S]*?>[\s\S]*?<\/feedbackinline>)/gm;

    var nodes = html.match(inlineChoiceRegex);

    var out = { options: [], feedbacks: []};

    if (!nodes) {
      return [];
    }

    for (var i = 0; i < nodes.length; i++) {
      var template = '<option value="${value}">${label}</option>';

      var node = angular.element(nodes[i]);

      var nodeContents = node.html();

      var feedbackNodes = nodeContents.match(feedbackRegex);

      if (feedbackNodes && feedbackNodes.length > 0) {
        var floatDirective = feedbackNodes[0].replace(/feedbackinline/g, "feedbackfloat");
        out.feedbacks.push(floatDirective);
      }

      var optionValue = nodeContents.replace(feedbackRegex, "");

      var option = template
        .replace("${value}", node.attr("identifier"))
        .replace("${label}", optionValue);

      out.options.push(option);
    }

    return out;
  };


  var compile = function (element, attrs, transclude) {


    var optionsAndFeedback = getOptionsAndFeedbacks(element);

    var html = ['<select ng-disabled="formSubmitted" class="small" ng-model="choice">'];

    //TODO: This isn't being picked up - leave it for now.
    if (element.attr('required') === "true") {
      html.push('<option value="">Choose..</option>');
    }

    html = html.concat(optionsAndFeedback.options);
    html.push('</select>');

    html = html.concat(optionsAndFeedback.feedbacks);
    element.html(html.join(""));
    return link;
  };

  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    compile: compile,
    //TODO - is this necessary?
    controller: function ($scope) {
      this.scope = $scope;
    }
  }
});


/**
 * Override feedback directive to use jQuery tooltip instead
 * @return {Object}
 */
var feedbackFloat = function (QtiUtils) {


  //<li ng-class="{true:'active', false:''}[currentPanel=='content']">
  return {
    restrict: 'E',
    template: '<span class="feedback-float" ng-class="getClass()"></span>',
    scope: true,
    replace: true,
    require: '^assessmentitem',
    link: function (scope, element, attrs) {
      console.log("feedback float!");
      var csFeedbackId = attrs["csfeedbackid"];

      scope.getTooltip = function () {
        return scope.feedback;
      };


      scope.$on('resetUI', function (event) {
        scope.feedback = "";
      });

      scope.getClass = function () {
        return scope.feedback ? "show" : "hide";
      };

      scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
        if (!responses || scope.isFeedbackEnabled() == false) return;

        var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
        if (feedback) {
          scope.feedback = feedback;

          $(element).tooltip({ title: scope.feedback});
        }
      });
      scope.feedback = "";
    },
    controller: function ($scope) {
      this.scope = $scope;
    }
  }
};

qtiDirectives.directive("feedbackfloat", feedbackFloat);

