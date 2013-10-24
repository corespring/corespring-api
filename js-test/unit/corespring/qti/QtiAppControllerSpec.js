describe('qtiDirectives.assessmentItem', function () {
  'use strict';

  var ctrl, scope, $httpBackend;

  beforeEach(module('corespring-services'));


  // Mock dependencies
  var MockAssessmentSessionService = function () {};


  var mockCall = function (params, obj, callback) {
    var data = {
      "settings": {
        "maxNoOfAttempts": 0,
        "highlightUserResponse": true,
        "highlightCorrectResponse": true,
        "showFeedback": true,
        "allowEmptyResponses": false,
        "submitCompleteMessage": "Submit Completed",
        "submitIncorrectMessage": "Submit Incorrect"
      }
    };
    callback(data);
  };

  MockAssessmentSessionService.create = mockCall;
  MockAssessmentSessionService.get = mockCall;


  beforeEach(function () {
    module(function ($provide) {
      $provide.value('AssessmentSessionService', MockAssessmentSessionService);
      $provide.value('Config', {itemId: "1"});
    });
  });

  var mockLocation = {
    absUrl: function () {
      return "/item/1/";
    }
  };

  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
    $httpBackend = _$httpBackend_;
    scope = $rootScope.$new();

    try {
      ctrl = $controller(QtiAppController, {
        $scope: scope,
        $location: mockLocation,
        Logger: {}
      });
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));


  describe('QtiAppController', function () {

    it('should watch the settings for changes', function () {
      expect(scope.settingsHaveChanged).toBe(false);

      scope.$apply(function () {
        scope.itemSession.settings.maxNoOfAttempts = 1;
      });
      expect(scope.settingsHaveChanged).toBe(true);

      scope.$apply(function () {
        scope.itemSession.settings.maxNoOfAttempts = 0;
      });
      expect(scope.settingsHaveChanged).toBe(false);
    });
  });

});