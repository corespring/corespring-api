function BrowseController($scope, $rootScope, $location, Config, LaunchConfigService, ItemFormattingUtils) {


  //Mixin ItemFormattingUtils
  angular.extend($scope, ItemFormattingUtils);

  $scope.$on('searchCompleted', function (event, items) {
    $rootScope.items = items;
  });

  $scope.selectItem = function (item) {
    $(".tooltip").remove();
    $location.url("/view/" + item.id);
  };

  $scope.settings = Config.settings;

  $scope.saveItem = function (onSaveCompleteCallback) {
    LaunchConfigService.save({id: $scope.quiz.id}, $scope.quiz, function (data) {
      $scope.quiz = data;

      if (onSaveCompleteCallback) onSaveCompleteCallback();
    });
  };

  $scope.change = function () {
    $scope.quiz.question.itemId = null;
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
    $rootScope.$broadcast("loadMore");
  };

}

BrowseController.$inject = ['$scope',
  '$rootScope',
  '$location',
  'Config',
  'LaunchConfigService',
  'ItemFormattingUtils'];
