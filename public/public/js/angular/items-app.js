var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'ui']);


function ItemsCtrl($scope, $timeout, Items, MultipleFieldValues) {
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
            $scope.itemState = "hasContent"
        });
    };

    //update the item list based on the search fields
    var updateItemList = function () {
        $scope.itemState = "loading";
        var searchFields = {};
        var grades = $scope.searchFields.grades;
        if (grades.length != 0) {
            if (grades.length == 1) {
                searchFields.gradeLevel = grades[0]
            } else {
                searchFields["$or"] = _.map(grades, function (grade) {
                    return {gradeLevel:grade}
                })
            }
        }
        var itemTypes = $scope.searchFields.itemTypes;
        if (itemTypes.length != 0) {
            if (itemTypes.length == 1) {
                searchFields.itemType = itemTypes[0]
            } else {
                searchFields["$or"] = _.map(itemTypes, function (iType) {
                    return {itemType:iType}
                })
            }
        }
        var primarySubjectIds = $scope.searchFields.primarySubjectIds;
        if (primarySubjectIds.length != 0) {
            if (primarySubjectIds.length == 1) {
                searchFields['subjects.primary'] = {"$oid":primarySubjectIds[0]}
            } else {
                searchFields['$or'] = _.map(primarySubjectIds, function (id) {
                    return {"subjects.primary":{"$oid":id}}
                })
            }
        }
        var isEmpty = function (obj) {
            for (var i in obj) {
                return false;
            }
            return true;
        };

        var getSearchFields = function() {
            if (!isEmpty(searchFields)) {
                return { q: JSON.stringify(searchFields)};
            }
            return {};
        };

        var fields = getSearchFields();
        $scope.getItems(fields);
    };

    //update items based on grades entered
    $scope.updateGradeSearch = function (grade) {
        var grades = $scope.searchFields.grades;
        var gradeIndex = grades.indexOf(grade);
        if (gradeIndex == -1) {
            grades.push(grade);
        } else {
            grades.splice(gradeIndex, 1);
        }
        updateItemList();
    };
    //update items based on item types entered
    $scope.updateItemTypeSearch = function (itemType) {
        var itemTypes = $scope.searchFields.itemTypes;
        var itemTypeIndex = itemTypes.indexOf(itemType);
        if (itemTypeIndex == -1) {
            itemTypes.push(itemType)
        } else {
            itemTypes.splice(itemTypeIndex, 1)
        }
        updateItemList();
    };
    //update items based on primary subject entered
    $scope.updatePrimarySubjectSearch = function (primarySubject) {
        var primarySubjectIds = $scope.searchFields.primarySubjectIds;
        var primarySubjectIndex = primarySubjectIds.indexOf(primarySubject.id);
        if (primarySubjectIndex == -1) {
            primarySubjectIds.push(primarySubject.id)
        } else {
            primarySubjectIds.splice(primarySubjectIndex, 1)
        }
        updateItemList();
    };

    $scope.getItems({});
}
ItemsCtrl.$inject = ['$scope', '$timeout', 'Items', 'MultipleFieldValues'];
