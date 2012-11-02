angular.module('itemResource', ['ngResource']).
    factory('Items', function($resource) {
        return $resource('http://localhost:port/public/items',{port:':9000'})
    });

angular.module('fieldValuesResource',['ngResource']).
    factory('FieldValues', function($resource) {
        return $resource('http://localhost:port/api/v1/field_values/:fieldValue',{port:':9000'})
    })