var app = angular.module('app', ['itemResource','fieldValuesResource']);


function ItemsCtrl($scope, FieldValues, Items) {
    $scope.searchFields = {
        grades:[],
        itemTypes:[],
        primarySubjectIds:[]
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
            console.log(JSON.stringify($scope.primarySubjects))
        })
        var gradeLevels = FieldValues.query({fieldValue: "gradeLevels"},function() {
            $scope.grades = gradeLevels.map(function(gradeLevel){return gradeLevel.key})
        })
        var itemTypes = FieldValues.query({fieldValue: "itemTypes"},function() {
            $scope.itemTypes = itemTypes.map(function(itemType){return itemType.key})
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
                searchFields["$or"] = grades.map(function(grade){return {gradeLevel : grade}})
            }
        }
        var itemTypes = $scope.searchFields.itemTypes
        if(itemTypes.length != 0) {
            if(itemTypes.length == 1){
                searchFields.itemType = itemTypes[0]
            }else{
                searchFields["$or"] = itemTypes.map(function(iType){return {itemType : iType}})
            }
        }
        var primarySubjectIds = $scope.searchFields.primarySubjectIds
        if(primarySubjectIds.length != 0) {
            if(primarySubjectIds.length == 1){
                searchFields['subjects.primary'] = {"$oid" : primarySubjectIds[0]}
            }else{
                searchFields['$or'] = primarySubjectIds.map(function(id){return {"subjects.primary": {"$oid" : id}}})
            }
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
//    $scope.updatePrimarySubjectSearch = function(primarySubject) {
//        var matchFlag = false;
//        for(var i = 0; i < $scope.primarySubjects.length; i++){
//            if($scope.primarySubjects[i].category == $scope.primarySubjectSearch){
//                matchFlag = true;
//                $scope.searchFields.primarySubjectId = {"$oid" : $scope.primarySubjects[i].id};
//            }
//        }
//        if(!matchFlag) $scope.searchFields.primarySubjectId = " "
//        updateItemList()
//    }
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
ItemsCtrl.$inject = ['$scope','FieldValues', 'Items']
