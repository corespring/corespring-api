/*
 * Controller for creating new item, in practice after the object is persisted
 * Control moves to the EditCtrl
 */
 angular.module('tagger').controller("CreateCtrl",['$scope', '$rootScope', '$routeParams','ItemService','NewItemTemplates',
 function($scope, $rootScope, $routeParams, ItemService, NewItemTemplates){

     $scope.closeCollectionWindow = function(){
          console.log("closeCollectionWindow")
          $rootScope.showCollectionWindow = false;
     }
 }])
