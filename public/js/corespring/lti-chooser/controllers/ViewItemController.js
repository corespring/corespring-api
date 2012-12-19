window.ltiChooser = (window.ltiChooser || {});

ltiChooser.ViewItemController = function ($scope, $rootScope, $routeParams, $location, LtiItemService, ServiceLookup){

  $scope.previewItemUrl = null;
  $rootScope.item = null;

  $scope.changePanel = function(name){
    $scope.currentSm = null;
    $scope.currentPanel = name;
  };

  $scope.changeSupportingMaterialPanel = function (sm) {
    $scope.changePanel(sm.name);
    $scope.currentSm = sm;
  };


  $scope.getItemUrl = function(){
    if (!$scope.item || $scope.currentPanel != 'preview') return null;
    return "/web/show-resource/" + $scope.item.id;
  };


  $scope.getSmSrc = function (sm, forPrinting) {
    var templateUrl = ServiceLookup.getUrlFor(forPrinting ? 'printResource' : 'renderResource');
    var key = $scope.item.id + "/" + sm.name;
    //empty it so we trigger a refresh
    return templateUrl.replace("{key}", key);
  };

  LtiItemService.get({id: $routeParams.itemId}, function onSuccess(data){
     $rootScope.item = data;
     $scope.changePanel('preview');
     $scope.previewItemUrl = $scope.getItemUrl();
  });

};


ltiChooser.ViewItemController.$inject = ['$scope', '$rootScope', '$routeParams', '$location', 'LtiItemService', 'ServiceLookup'];