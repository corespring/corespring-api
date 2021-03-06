angular.module('tagger.services').service('V2SearchService', ['$rootScope', '$http',
  function ($rootScope, $http) {

    var results = [];
    var query = {};

    function toQuery(params) {
      return {
        sort: _.map(params.sort, function(direction, field) {
          var sort = {};
          sort[field] = (direction == 1) ? "asc" : "desc";
          return sort;
        })[0],
        text: _.isEmpty(params.searchText) ? undefined : params.searchText,
        contributors: _.map(params.contributor, function(contributor) {
          return contributor.name;
        }),
        collections: _.map(params.collection, function(collection) {
          return collection.id;
        }),
        gradeLevels: _.map(params.gradeLevel, function(gradeLevel) {
          return gradeLevel.key;
        }),
        itemTypes: _.map(params.itemType, function(itemType) {
          return itemType.key;
        }),
        widgets: _.map(params.widgets, function(widget) {
          return widget.key;
        }),
        requiredPlayerWidth: params.requiredPlayerWidth,
        published: function() {
          function hasKey(key) {
            return _.find(params.publishStatuses, function(status) {
              return status.key === key;
            }) !== undefined;
          }
          var published = hasKey('published') ? 1 : 0;
          var draft = hasKey('draft') ? 1 : 0;
          return (published ^ draft) ? (published == 1) : undefined;
        }(),
        workflows: _.filter(['setup', 'tagged', 'standardsAligned', 'qaReview'], function(workflow) {
          return _.find(params.statuses, function(status) {
            return status.key === workflow;
          }) !== undefined;
        })
      };
    }

    function loading(promise) {
      $rootScope.$broadcast('onNetworkLoading');
      promise.success(callback).error(callback);
      return promise;

      function callback() {
        $rootScope.$broadcast('onNetworkComplete');
      }
    }

    function makeRequest(query){
      return loading($http.get('/web/item-search?query=' + encodeURIComponent(JSON.stringify(query))));
    }

    function Results() {
      var self = this;
      this.resultCount = 0;
      this.itemDataCollection = [];
      this.count = function() {
        return 0;
      };

      this.loadMore = function(callback) {
        query.offset = self.itemDataCollection.length;
        makeRequest(query).success(function(result) {
          self.itemDataCollection = self.itemDataCollection.concat(result.hits);
          callback(self.itemDataCollection);
        });
      };

      this.search = function(params, callback) {
        query = toQuery(params);
        makeRequest(query).success(function(result) {
          self.itemDataCollection = result.hits;
          self.resultCount = result.total;
          $rootScope.$broadcast('onSearchCountComplete', self.resultCount);
          callback(self.itemDataCollection);
        });
      };

      this.resetDataCollection = function() {
        self.itemDataCollection = [];
      };
    };

    return new Results();
}]);
