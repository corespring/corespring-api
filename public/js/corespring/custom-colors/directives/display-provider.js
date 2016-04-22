angular.module('customColors.directives').factory('DisplayProvider', ['$http', function($http) {

  var defaults = undefined;

  return {

    get: function(callback) {
      $http({
        method: 'GET',
        url: "/api/v2/organizations/display-config"
      }).then(function success(response) {
        callback(response.data);
      }, function error(response) {
        console.log(response);
      });
    },

    set: function(values, callback) {
      if (confirm("Are you sure? These changes will apply to all items.")) {
        $http({
          method: 'PUT',
          url: "/api/v2/organizations/display-config",
          data: values
        }).then(function success(response) {
          callback(response.data);
        }, function error() {
          window.alert("There was an error saving your preferences.");
        });
      }
    },

    defaults: function(callback) {
      if (defaults !== undefined) {
        callback(defaults);
      } else {
        $http({
          method: 'GET',
          url: "/api/v2/organizations/display-config/default"
        }).then(function success(response) {
          defaults = response.data;
          callback(response.data);
        }, function error(response) {
          console.log(response);
          window.alert("There was an error reading the defaults.");
        });
      }
    }

  };

}]);