describe('lti-chooser.BrowseController', function () {
  'use strict';

  var ctrl, scope, $httpBackend;

 // Mock dependencies
  var MockLaunchConfigService = function () {};

  var mockCall = function (params, obj, callback) {
    callback(obj);
  };

  MockLaunchConfigService.save = mockCall;

  beforeEach(module('corespring-utils'));
  beforeEach(module('corespring-services'));

  var mockConfig = {id:"1", question: { itemId: "1" }, participants: []};

  beforeEach(function () {
    module(function ($provide) {
      $provide.value('LaunchConfigService', MockLaunchConfigService);
      $provide.value('Config', mockConfig);
    });
  });

  var mockLocation = {
    lastUrl : null,
    absUrl: function () {
      return "/item/1/";
    },
    url: function(url){
     this.lastUrl = url; 
    }
  };

  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
    $httpBackend = _$httpBackend_;
    scope = $rootScope.$new();

    scope.assessment = mockConfig;

    try {
      ctrl = $controller(BrowseController, {
        $scope: scope,
        $location: mockLocation
      });
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));


  describe('BrowseController', function () {

    it('should select item', function () {
      scope.selectItem( { id: "one"});
      expect(mockLocation.lastUrl).toBe("/view/one");
    });

    it('should save item', function(){
      var saveSuccessful = false;
      scope.saveItem( function(){
        saveSuccessful = true;
      });
      expect(saveSuccessful).toBe(true);
    });

    it('should change', function(){
      scope.change();
      expect(scope.assessment.question.itemId).toBeNull();
      expect(scope.mode).toBe('start');
    });

    it('should get no of assignments', function(){
      var count = scope.getNumberOfAssignments({assignments: []});
      expect(count).toBe(0);
      var count = scope.getNumberOfAssignments({assignments: ["one"]});
      expect(count).toBe(1);
    });

    it('should select column', function(){
      scope.predicate = 'other';
      scope.reverse = true;
      expect(scope.selectColumn('type')).toBe('sortableColumn');
      expect(scope.selectColumn('other')).toBe('selectedColumnDesc');
      scope.reverse = false;
      expect(scope.selectColumn('other')).toBe('selectedColumnAsc');
    });
  });

});