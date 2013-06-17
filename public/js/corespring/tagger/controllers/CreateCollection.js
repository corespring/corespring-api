 angular.module('tagger').directive('contenteditable', function(Collection){ return {
    restrict: 'A',
    scope: {collectionId: '@collectionId', collectionName: '=collectionName', newAlert: '=newAlert', createSortedCollection: '=createSortedCollection'},
    link: function(scope,element,attrs){

      element.html(scope.collectionName)

      // view -> model
      element.bind('blur', function() {

        Collection.update({id: scope.collectionId},{name: element.html()},function(data){
            scope.collectionName = data.name
            scope.newAlert("alert alert-success", "Successfully edited collection name");
            scope.createSortedCollection();
        },function(err){
            scope.newAlert("alert alert-error", "Error editing collection name");
            console.log("error update collection name: "+err)
            element.html(scope.collectionName)
        })
      });

      element.bind('keyup', function(evt){
        if(evt.which == 13){  //enter key was pressed
            element.html(element.context.innerText);
            element.blur();
        }
      })

//      // model -> view
//      ctrl.$render = function() {
//        element.html(ctrl.$viewValue);
//      };
//
//      // load init value from DOM
//      ctrl.$setViewValue(element.html());

    }
 }})
 //controller
 angular.module('tagger').controller("CreateCollection",['$scope', '$rootScope', 'Collection', 'UserInfo',
 function($scope, $rootScope, Collection, UserInfo){
   $scope.orgName = UserInfo.org.name;
   $scope.newAlert = function(cssclass,message){
    $scope.alertClass = cssclass;
    $scope.alertMessage = message;
   }
   $scope.newAlert("alert","");
   //this looks kind of weird. this is all for opening and closing the collections modal correctly between MainNavController and this controller
   $rootScope.$watch('collectionsWindowRoot',function(){
     $scope.collectionsWindow = $rootScope.collectionsWindowRoot
   })
   $scope.closeCollectionWindow = function(){
     $scope.collectionsWindow = false;
   }
   $scope.$watch('collectionsWindow',function(){
    if(!$scope.collectionsWindow && $rootScope.collectionsWindowRoot) $rootScope.collectionsWindowRoot = false;
    else if($scope.collectionsWindow && $rootScope.collections){  //if dialog is shown, populate collections table with collections that org has write access too
        $scope.collections = _.filter($rootScope.collections, function(c){
            return (c.access & 3) == 3
        })
    }
   })
   $scope.createCollection = function(collectionName){
     if(collectionName){
       Collection.create({},{name:collectionName},function(data){
           $rootScope.collections.push(data);
           $scope.collections.push(data);
           $scope.searchParams.collection.push(data)
           $('#newcollection').val('');
           $scope.createSortedCollection();
           $scope.newAlert('alert alert-success',"Successfully created collection")
       },function(err){
           $scope.newAlert('alert alert-error', "Error occurred when creating a collection");
           console.log("create collection: error: " + err);
       })
     }
   };
   $scope.deleteCollection = function(collId){
     Collection.remove({id: collId},function(data){
        //todo: add success message
        $rootScope.collections = _.filter($rootScope.collections, function(c){
            return c.id !== collId
        })
        $scope.collections = _.filter($scope.collections, function(c){
            return c.id !== collId
        })
        if($scope.searchParams.collection){
          $scope.searchParams.collection = _.filter($scope.searchParams.collection, function(searchcoll){
              var found = _.find($rootScope.collections, function(c){
                return c.id == searchcoll.id;
              });
              return found;
          });
        }
        $scope.createSortedCollection();
        $scope.newAlert('alert alert-success',"Successfully deleted collection");
     },function(err){
        $scope.newAlert('alert alert-error', "Error deleting collection");
        console.log("error deleting collection: "+err);
     })
   }
 }])