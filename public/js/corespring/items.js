angular.module('itemResource', ['ngResource']).
    factory('Items', function($resource) {
        return $resource('items')
    });

angular.module('fieldValuesResource',['ngResource']).
    factory('FieldValues', function($resource) {
        return $resource('api/v1/field_values/:fieldValue')
    })