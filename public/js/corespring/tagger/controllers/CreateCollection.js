 angular.module('tagger').controller("CreateCollection",['$scope', '$rootScope',
 function($scope, $rootScope){

     $scope.closeCollectionWindow = function(){
          $rootScope.showCollectionWindow = false;
     }
 }])