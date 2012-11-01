var app = angular.module('app', ['itemResource','fieldValuesResource']);


function ItemsCtrl($scope, FieldValues, Items) {
    //set the items to it's initial state (grab everything with no search params).
    if($scope.items !== undefined){
        $scope.items.length = 0
        var newItems = Items.query({},function(){
            $scope.items.push(newItems)
        })
    }else{
        $scope.items = Items.query()
    }
    $scope.rootUrl = "http://app.corespring.org";
    $scope.searchFields = {
        grades:[],
        itemTypes:[],
        primarySubject:""
    };
    $scope.primarySubjects = [];//FieldValues.primarySubjects;
    $scope.grades = [];
    $scope.itemTypes = [];//FieldValues.itemTypes
    //set the field values based on the json object
    var fieldValues = FieldValues.get({},function() {
        //need a loop instead of map because subject may be empty
        for(var i = 0, x = 0; i < fieldValues.primarySubjects.length; i++){
            var subject = fieldValues.primarySubjects[i].subject;
            if(subject && subject != "Other"){
                $scope.primarySubjects[x] = subject;
                x++;
            }
        }
        $scope.grades = fieldValues.gradeLevels.map(function(gradeLevel){return gradeLevel.key})
        $scope.itemTypes = fieldValues.itemTypes.map(function(itemType){return itemType.key})
    })
    //update the item list based on the search fields
    var updateItemList = function() {
        var searchFields = {}
        var grades = $scope.searchFields.grades
        if(grades.length != 0) {
            if(grades.length == 1){
                searchFields.gradeLevel = grades[0]
            }else{
                searchFields.gradeLevel = {"$or" : JSON.stringify(grades)}
            }
        }
        var itemTypes = $scope.searchFields.itemTypes
        if(itemTypes.length != 0) {
            if(itemTypes.length == 1){
                searchFields.itemType = itemTypes[0]
            }else{
                searchFields.itemType = {"$or" : JSON.stringify(itemTypes)}
            }
        }
        var primarySubject = $scope.searchFields.primarySubject
        if(primarySubject) {
            searchFields['primarySubject'] = primarySubject
        }
        var isEmpty = function(obj){
            for(var i in obj) {return false;}
            return true;
        }
        if(!isEmpty(searchFields)){
            //TODO add query to search
        }
    }
    //update items based on grades entered
    $scope.updateGradeSearch = function(event, grade) {
        var grades = $scope.searchFields.grades
        var gradeIndex = grades.indexOf(grade)
        if(gradeIndex == -1){
            grades.push(grade)
            event.srcElement.setAttribute("class","btn btn-primary")
        }else{
            grades.splice(gradeIndex,1)
            event.srcElement.setAttribute("class","btn")
        }
        updateItemList()
        console.log(JSON.stringify(grades))
    }
    //update items based on item types entered
    $scope.updateItemTypeSearch = function(itemType) {
        var itemTypes = $scope.searchFields.itemTypes
        var itemTypeIndex = itemTypes.indexOf(itemType)
        if(itemTypeIndex == -1){
            itemTypes.push(itemType)
        }else{
            itemTypes.splice(itemTypeIndex,1)
        }
        updateItemList()
        console.log(JSON.stringify(itemTypes))
    }
    //update items based on primary subject entered
    $scope.updatePrimarySubjectSearch = function(event) {
        $scope.searchFields.primarySubject = event.srcElement.options[event.srcElement.selectedIndex].innerHTML
        updateItemList()
        console.log(JSON.stringify($scope.searchFields.primarySubject ))
    }
}
ItemsCtrl.$inject = ['$scope','FieldValues', 'Items']
