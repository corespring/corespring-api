angular.module('itemResource', ['ngResource']).
    factory('Items', function($resource) {
        return $resource('/example-content/items')
    });

angular.module('fieldValuesResource',['ngResource']).
    factory('FieldValues', function($resource) {
        return $resource('/api/v1/field_values/:fieldValue')
    })