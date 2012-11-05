var app = angular.module('app', ['itemResource','fieldValuesResource']);


function ItemsCtrl($scope, FieldValues, Items) {
    $scope.searchFields = {
        grades:[],
        itemTypes:[],
        primarySubject:" "
    };
    $scope.primarySubjects = [];//FieldValues.primarySubjects;
    $scope.grades = [];
    $scope.itemTypes = [];//FieldValues.itemTypes
    //set the items to it's initial state (grab everything with no search params).
    if($scope.items !== undefined){
        $scope.items.length = 0
        var newItems = Items.query({},function(){
            newItems.map(function(item){$scope.items.push(item)})
        })
    }else{
        var newItems = Items.query({},function(){
            $scope.items = newItems
        })
    }
    //set the field values based on the json object
    //there seems to be a bug in angularjs where multiple queries at once just time out. Hence, the embedded queries. Once one is done, the other starts.
    (function(){
        var gradeLevels = FieldValues.query({fieldValue: "gradeLevels"},function() {
            $scope.grades = gradeLevels.map(function(gradeLevel){return gradeLevel.key})
                    var subjects = FieldValues.query({fieldValue: "subject"},function() {
                        //need a loop instead of map because subject may be empty
                        for(var i = 0, x = 0; i < subjects.length; i++){
                            var subject = subjects[i].subject;
                            if(subject && subject != "Other"){
                                $scope.primarySubjects[x] = subject;
                                x++;
                            }
                        }
                                var itemTypes = FieldValues.query({fieldValue: "itemTypes"},function() {
                                    $scope.itemTypes = itemTypes.map(function(itemType){return itemType.key})
                                })
                    })
        })
    })()
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
        if(primarySubject != " ") {
            searchFields['primarySubject.subject'] = primarySubject
        }
        var isEmpty = function(obj){
            for(var i in obj) {return false;}
            return true;
        }
        if(!isEmpty(searchFields)){
            console.log("searchFields: "+JSON.stringify(searchFields))
            var newItems = Items.query({q : JSON.stringify(searchFields)},function(){
                console.log("newItems: "+JSON.stringify(newItems))
                $scope.items.length = 0
                newItems.map(function(item){$scope.items.push(item)})
            })
        }else{
            var newItems = Items.query({},function(){
                $scope.items = newItems
            })
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
    }
    //update items based on primary subject entered
    $scope.updatePrimarySubjectSearch = function() {
        $scope.searchFields.primarySubject = $scope.primarySubjectSearch
        updateItemList()
    }
}
ItemsCtrl.$inject = ['$scope','FieldValues', 'Items']
