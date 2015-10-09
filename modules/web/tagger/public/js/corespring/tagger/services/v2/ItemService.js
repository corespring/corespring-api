angular.module('tagger.services')
  .factory('V2ItemService', ['$http',
    function($http) {

      function V2ItemService() {
        this.create = function(data, onSuccess, onError) {
          $http.post('/api/v2/items', data)
            .success(onSuccess)
            .error(onError);
        };

        this.publish = function(params, onSuccess, onError) {
          var url = "/api/v2/items/:id/publish".replace(":id", params.id);

          $http.put(url, {})
            .success(onSuccess)
            .error(onError);
        };

        this.delete = function(params, onSuccess, onError) {
          var url = "/api/v2/items/:id".replace(":id", params.id);
          $http.delete(url, {})
            .success(onSuccess)
            .error(onError);
        };

        this.clone = function(params, onSuccess, onError) {
          var url = "/api/v2/items/:id/clone".replace(":id", params.id);

          $http.post(url, {})
            .success(onSuccess)
            .error(onError);
        };

      }

      return new V2ItemService();


    }]);

