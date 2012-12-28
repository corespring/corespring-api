function BrowseController($scope, $rootScope, $location, Config, LaunchConfigService, ItemFormattingUtils) {

  //Mixin ItemFormattingUtils
  angular.extend($scope, ItemFormattingUtils);

  $scope.showTip = true;

  $scope.$on('beginSearch', function (event, items) {
  });

  $scope.$on('searchCompleted', function (event, items) {
    $rootScope.items = items;
  });

  $scope.selectItem = function (item) {
    $(".tooltip").remove();
    $location.url("/view/" + item.id);
  };


  $scope.settings = Config.settings;

  $scope.saveItem = function (onSaveCompleteCallback) {
    LaunchConfigService.save({id: $scope.config.id}, $scope.config, function (data) {
      $scope.config = data;
      $scope.updateHasItem();

      if (onSaveCompleteCallback) onSaveCompleteCallback();
    });
  };

  $scope.change = function () {
    $scope.config.itemId = null;
    $scope.mode = 'start';
  };

  $scope.getNumberOfAssignments = function (config) {
    if (config && config.assignments) return config.assignments.length;
    return 0;
  };

  $scope.selectColumn = function (p) {
    if (p != $scope.predicate) {
      return "sortableColumn";
    }

    if ($scope.reverse) {
      return "selectedColumnDesc";
    } else {
      return "selectedColumnAsc";
    }
  };

  $scope.loadMore = function(){
    console.log("loadMore");
    $rootScope.$broadcast("loadMore");
  }

}

BrowseController.$inject = ['$scope',
  '$rootScope',
  '$location',
  'Config',
  'LaunchConfigService',
  'ItemFormattingUtils'];
