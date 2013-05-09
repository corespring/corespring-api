var chooseLabel = "Choose...";

angular.module('qti.directives').directive('inlinechoiceinteraction', function (QtiUtils) {


  var getOptionsAndFeedbacks = function (element) {

    var html = element.html();

    var inlineChoiceRegex = /<:*inlinechoice[\s\S]*?>([\s\S]*?)<\/:*inlinechoice>/gmi;
    var feedbackRegex = /(<:*feedbackinline[\s\S]*?>[\s\S]*?<\/:*feedbackinline>)/gmi;

    var nodes = html.match(inlineChoiceRegex);

    var out = { options: [], feedbacks: [], labels: []};

    if (!nodes) {
      return [];
    }

    nodes = $(element).children('inlinechoice');

    for (var i = 0; i < nodes.length; i++) {
      var template = '<li><a ng-click="click(\' ' + i + ' \', \'${value}\')">$label</a></li>';
      var node = angular.element(nodes[i]);
      var nodeContents = node.html();

      var feedbackNodes = nodeContents.match(feedbackRegex);
      if (feedbackNodes && feedbackNodes.length > 0) {
        var floatDirective = feedbackNodes[0].replace(/<:*feedbackinline/gim, "<span class='feedbackfloat'").replace(/\/:*feedbackinline/gim, "/span");
        out.feedbacks.push(floatDirective);
      }

      var optionValue = nodeContents.replace(feedbackRegex, "");

      var option = template
        .replace("${value}", node.attr("identifier"))
        .replace(/\$label/gi, optionValue);

      out.labels.push("<span ng-show='selectedIdx=="+i+"'>"+optionValue+"</span>");

      out.options.push(option);
    }
    out.labels.push("<span ng-show='selectedIdx==-1'>"+chooseLabel+"</span>");

    return out;
  };


  var compile = function (element, attrs, transclude) {


    var optionsAndFeedback = getOptionsAndFeedbacks(element);

    var html = [
      '<div class="btn-group" style="display: inline-block">',
      '<a class="btn dropdown-toggle" ng-class="{disabled: formSubmitted}" data-toggle="dropdown" href="#">'
    ].concat(optionsAndFeedback.labels).concat([
      '<span class="caret"></span></a>',
      '<ul class="dropdown-menu">'
    ]);

    //TODO: This isn't being picked up - leave it for now.
    if (element.attr('required') === "true") {
      html.push('<option value="">Choose..</option>');
    }

    html = html.concat(optionsAndFeedback.options);
    html.push('</ul></div>');

    html = html.concat(optionsAndFeedback.feedbacks);
    element.html(html.join(""));

    return function ($scope, element, attrs, AssessmentItemCtrl) {
      $scope.labels = optionsAndFeedback.labels;
      $scope.selectedIdx = -1;
      AssessmentItemCtrl.registerInteraction(element.attr('responseIdentifier'), 'inline');

      var modelToUpdate = attrs["responseidentifier"];

      $scope.click = function (label, value) {
        $scope.selected = $scope.labels[Number(label)];
        $scope.selectedIdx = Number(label);
        $scope.choice = value;
        $(element).find('.dropdown-toggle').dropdown('toggle');
      }

      $scope.$watch('choice', function (newValue) {
        AssessmentItemCtrl.setResponse(modelToUpdate, newValue);
      });

      $scope.$on('resetUI', function (event) {
        element
          .removeClass('correct-response')
          .removeClass('incorrect-response')
          .removeClass('received-response');
      });

      $scope.$on('unsetSelection', function (event) {
        $scope.choice = "";
        $scope.selectedIdx = -1;
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
          .removeClass('incorrect-response')
          .removeClass('received-response');

        if (responses.length == 0) {
          element.addClass('received-response');
        }
        else if (isCorrect) {
          if ($scope.highlightCorrectResponse() || $scope.highlightUserResponse()) {
            element.addClass('correct-response');
          }
        }
        else {
          if ($scope.highlightUserResponse()) {
            element.addClass('incorrect-response');
          }
        }
      });
    };
  };

  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    compile: compile,
    controller: function ($scope) {
      this.scope = $scope;
      $scope.selected = chooseLabel;
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
    restrict: 'EAC',
    template: '<span class="feedback-float" ng-class="getClass()"></span>',
    scope: true,
    replace: true,
    require: '^assessmentitem',
    link: function (scope, element, attrs) {
      var csFeedbackId = attrs["csfeedbackid"];

      scope.getTooltip = function () {
        return scope.feedback;
      };

      scope.$on('resetUI', function (event) {
        scope.feedback = '';
      });

      scope.$on('unsetSelection', function (event) {
        $(element).tooltip('destroy');
      });

      scope.$on('controlBarChanged', function() {
        $(element).tooltip('destroy');
      });

      scope.getClass = function () {
        return scope.feedback ? "show" : "hide";
      };

      scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
        if (!responses || scope.isFeedbackEnabled() == false || !scope.highlightUserResponse()) return;

        var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
        if (feedback) {
          scope.feedback = feedback;
          $(element).tooltip({ title: scope.feedback, trigger: 'manual'});
          setTimeout(function () {
            $(element).tooltip('show');
          }, 100);
        }
      });
      scope.feedback = "";
    },
    controller: function ($scope) {
      this.scope = $scope;
    }
  }
};

angular.module('qti.directives').directive("feedbackfloat", feedbackFloat);

