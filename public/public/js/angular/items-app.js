var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'ui']);


function ItemsCtrl($scope, $timeout, Items, MultipleFieldValues) {

    var query = new com.corespring.mongo.MongoQuery();

    $scope.searchFields = {
        grades:[],
        itemTypes:[],
        primarySubjectIds:[]
    };
    $scope.itemState = "loading";
    $scope.primarySubjects = [];//FieldValues.primarySubjects;
    $scope.grades = [];
    $scope.itemTypes = [];//FieldValues.itemTypes
    //set the field values based on the json object

    var fieldNames = "subject,gradeLevels,itemTypes";
    var fieldOptions = {
        subject:{
            q:{
                $or:[
                    { category:"Mathematics", subject:"" },
                    { category:"Science", subject:"" },
                    { category:"English Language Arts", subject:"" }
                ]
            }
        }
    };

    /**
     * Map function for extracting a key
     * @param keyValue
     * @return {*}
     */
    var getKey = function (keyValue) {
        if (!keyValue) {
            return "";
        }
        return keyValue.key;
    };

    MultipleFieldValues.multiple(
        {
            fieldNames:fieldNames,
            fieldOptions:JSON.stringify(fieldOptions)
        }, function (data) {

            $scope.primarySubjects = data.subject;
            $scope.grades = _.map(data.gradeLevels, getKey);
            $scope.itemTypes = _.map(data.itemTypes, getKey);

        });

    $scope.getItems = function(query){
        Items.query(query, function (data) {
            $scope.items = data;
            $scope.itemState = "hasContent";
        });
    };

    /**
     * Build a query object from the search field array.
     * builds either a simple query object eg: {key: "value"}
     * or an $or query eg: { $or: [{key: "value1"}, {key: "value2"}]}
     */
    var makeQueryFromArray = function(array, key, makeFn){

        if(!array || array.length === 0) return null;

        //default makeFn to just pass the value through
        makeFn = (makeFn || function(v){return v;});
       
        //a curried js function
        var keyFn = function(key){
            return function(val){
                var o = {};
                o[key] = makeFn(val);
                return o;
            };
        };

        var output = {};

        if (array.length === 1) {
            output[key] = makeFn(array[0]);
        } else {
            output["$or"] = _.map(array, keyFn(key) );
        }
        return output;
    };
    
    //update the item list based on the search fields
    var updateItemList = function () {
        $scope.itemState = "loading";

        var gradeQuery = makeQueryFromArray($scope.searchFields.grades,"gradeLevel");
        var typeQuery = makeQueryFromArray($scope.searchFields.itemTypes,"itemType");
        var makeOid = function(id){ return { "$oid" : id}; };
        var subjectQuery = makeQueryFromArray($scope.searchFields.primarySubjectIds,"subjects.primary", makeOid);

        var andQuery = query.and(gradeQuery, typeQuery, subjectQuery);
        var finalQuery = andQuery.$and.length === 0 ? {} : { q: JSON.stringify(andQuery) };
        $scope.getItems(finalQuery);
    };

    $scope.toggleSearchField = function(fieldArray, value){
        try{
            var index = _.indexOf(fieldArray,value);
            if (index == -1) {
                fieldArray.push(value);
            } else {
                fieldArray.splice(index, 1);
            }
            updateItemList();

        } catch (e){
            throw "Error in toggleSearchField: " + e;
        }
    };

    //update items based on grades entered
    $scope.updateGradeSearch = function (grade) {
        $scope.toggleSearchField($scope.searchFields.grades, grade);
    };

    //update items based on item types entered
    $scope.updateItemTypeSearch = function (itemType) {
        $scope.toggleSearchField($scope.searchFields.itemTypes,itemType);
    };

    //update items based on primary subject entered
    $scope.updatePrimarySubjectSearch = function (primarySubject) {
        $scope.toggleSearchField($scope.searchFields.primarySubjectIds, primarySubject.id);
    };

    $scope.getItems({});
}
ItemsCtrl.$inject = ['$scope', '$timeout', 'Items', 'MultipleFieldValues'];
