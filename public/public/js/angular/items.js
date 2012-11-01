angular.module('itemResource', ['ngResource']).
    factory('Items', function($resource) {
        return $resource('conf/items.json')
    });

angular.module('fieldValuesResource',['ngResource']).
    factory('FieldValues', function($resource) {
        return $resource('conf/fieldValues.json')
    })