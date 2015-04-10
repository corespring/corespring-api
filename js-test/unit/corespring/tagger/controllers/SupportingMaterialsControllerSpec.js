describe('SupportingMaterialsController', function () {

  var routeParams, scope, ctrl, location, http;

  var supportingMaterial;

  beforeEach(function () {
    module(function ($provide) {
      $provide.value('AccessToken', {token: "1"});
    });
  });
  
  beforeEach(module('tagger.services'));

  beforeEach(inject(function ($rootScope, $controller, $location, $http) {
    scope = $rootScope.$new();
    scope.itemData = {
      latest: true
    };
    routeParams = { itemId: '1'};
    location = $location;
    http = $http;

    supportingMaterial = {
      query: jasmine.createSpy('query')
    };

    rootScope = {
      $broadcast: jasmine.createSpy('$broadcast')
    };

    try {
      ctrl = $controller(
        SupportingMaterialsController, 
        {$scope: scope, 
          $rootScope: rootScope,
          $routeParams: routeParams, 
          $location: location, 
          'SupportingMaterial' : supportingMaterial,
          Logger: {}});
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));


  describe('initialisation', function(){
    it('init correctly', inject(function () {
      expect(ctrl).not.toBeNull();
      expect(supportingMaterial.query).toHaveBeenCalled();
    }));
  });

  describe('editResource', function(){

    it('passes in the params for \'enterEditor\'', function(){

      scope.editResource({ name: 'resource'});

      expect(rootScope.$broadcast).toHaveBeenCalledWith(
        'enterEditor',
        { name: 'resource'},
        true, 
        scope.getUrls(scope.itemId, 'resource'),
        [],
        '1',
        true
        );
    });
  });

});
