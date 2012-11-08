var app = angular.module('app', ['itemResource','fieldValuesResource','ui']);


function ItemsCtrl($scope, $timeout, FieldValues, Items) {
    $scope.searchFields = {
        grades:[],
        itemTypes:[],
        primarySubjectIds:[]
    };
    $scope.itemState = "loading"
    $scope.primarySubjects = [];//FieldValues.primarySubjects;
    $scope.grades = [];
    $scope.itemTypes = [];//FieldValues.itemTypes
    //set the field values based on the json object
    (function(){
        var subjects = FieldValues.query({fieldValue: "subject"},function() {
            var categoriesUsed = ['Mathematics', 'Science', 'English Language Arts']
            var subjectToBeUsed = function(subject){
                for(var i = 0; i < categoriesUsed.length; i++){
                    if(subject.category == categoriesUsed[i]) return true;
                }
                return false;
            }
            //need a loop instead of map because subject is funky
            for(var i = 0, x = 0; i < subjects.length; i++){
                if(subjects[i].subject == "" && subjectToBeUsed(subjects[i])){
                    $scope.primarySubjects[x] = subjects[i];
                    x++;
                }
            }
        })
        var gradeLevels = FieldValues.query({fieldValue: "gradeLevels"},function() {
            $scope.grades = _.map(gradeLevels,function(gradeLevel){return gradeLevel.key})
        })
        var itemTypes = FieldValues.query({fieldValue: "itemTypes"},function() {
            $scope.itemTypes = _.map(itemTypes,function(itemType){return itemType.key})
        })
    })()
    //set a timeout before retrieving items. required so app doesn't crash
    $timeout(function(){
        //set the items to it's initial state (grab everything with no search params).
        if($scope.items !== undefined){
            $scope.items.length = 0
            var newItems = Items.query({},function(){
                _.map(newItems,function(item){$scope.items.push(item)})
                $scope.itemState="hasContent"
            })
        }else{
            var newItems = Items.query({},function(){
                $scope.items = newItems
                $scope.itemState = "hasContent"
            })
        }
    },500)
    //update the item list based on the search fields
    var updateItemList = function() {
        $scope.itemState = "loading"
        var searchFields = {}
        var grades = $scope.searchFields.grades
        if(grades.length != 0) {
            if(grades.length == 1){
                searchFields.gradeLevel = grades[0]
            }else{
                searchFields["$or"] = _.map(grades,function(grade){return {gradeLevel : grade}})
            }
        }
        var itemTypes = $scope.searchFields.itemTypes
        if(itemTypes.length != 0) {
            if(itemTypes.length == 1){
                searchFields.itemType = itemTypes[0]
            }else{
                searchFields["$or"] = _.map(itemTypes,function(iType){return {itemType : iType}})
            }
        }
        var primarySubjectIds = $scope.searchFields.primarySubjectIds
        if(primarySubjectIds.length != 0) {
            if(primarySubjectIds.length == 1){
                searchFields['subjects.primary'] = {"$oid" : primarySubjectIds[0]}
            }else{
                searchFields['$or'] = _.map(primarySubjectIds,function(id){return {"subjects.primary": {"$oid" : id}}})
            }
        }
        var isEmpty = function(obj){
            for(var i in obj) {return false;}
            return true;
        }
        console.log("searchFields: "+JSON.stringify(searchFields))
        if(!isEmpty(searchFields)){
            var newItems = Items.query({q : JSON.stringify(searchFields)},function(){
                $scope.items.length = 0
                if(newItems.length == 0){
                    $scope.itemState = "noContent"
                }else{
                    _.map(newItems,function(item){$scope.items.push(item)})
                    $scope.itemState = "hasContent"
                }
            })
        }else{
            var newItems = Items.query({},function(){
                $scope.items = newItems
                $scope.itemState = "hasContent"
            })
        }
    }
    //update items based on grades entered
    $scope.updateGradeSearch = function(grade) {
        var grades = $scope.searchFields.grades
        var gradeIndex = grades.indexOf(grade)
        if(gradeIndex == -1){
            grades.push(grade)
           // event.srcElement.setAttribute("class","btn btn-primary")
        }else{
            grades.splice(gradeIndex,1)
          //  event.srcElement.setAttribute("class","btn")
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
    $scope.updatePrimarySubjectSearch = function(primarySubject) {
        var primarySubjectIds = $scope.searchFields.primarySubjectIds
        var primarySubjectIndex = primarySubjectIds.indexOf(primarySubject.id)
        if(primarySubjectIndex == -1){
            primarySubjectIds.push(primarySubject.id)
        }else{
            primarySubjectIds.splice(primarySubjectIndex,1)
        }
        updateItemList()
    }
}
ItemsCtrl.$inject = ['$scope', '$timeout', 'FieldValues', 'Items']
