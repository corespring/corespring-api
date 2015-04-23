(function(root) {

  function SubNavController($scope, $rootScope, CollectionManager, Contributor){

    function init(){
      loadCollections();
      loadContributors();
      var defaultsFactory = new com.corespring.model.Defaults();
      $scope.gradeLevelDataProvider = defaultsFactory.buildNgDataProvider('gradeLevels');
      $scope.itemTypeDataProvider = defaultsFactory.buildNgDataProvider('v2ItemTypes');
      $scope.flatItemTypeDataProvided = $scope.itemTypeDataProvider;

      $scope.statuses = [
        { label: 'Setup', key: 'setup' }, 
        { label: 'Tagged', key: 'tagged' }, 
        { label: 'Standards Aligned', key: 'standardsAligned' }, 
        { label: 'QA Review', key: 'qaReview' },
        { label: 'Exact Match', key: 'exactMatch' }];
      
      $scope.publishStatuses = [
        { label: 'Published', key: 'published' }, 
        { label: 'Draft', key: 'draft' } ];
    }

    function loadCollections() {

      $scope.$watch(
        function() {
          return CollectionManager.sortedCollections;
        }, 
        function(newValue, oldValue) {
          if (newValue) {
            $scope.sortedCollections = newValue;
            if (!$rootScope.searchParams.collection && $scope.sortedCollections) {
              $rootScope.searchParams.collection = _.clone($scope.sortedCollections[0].collections);
            }
            $scope.search();
          }
        }, 
        true);

      CollectionManager.init();
    }

    function loadContributors() {
      Contributor.get({}, 
        function(data) {
          $scope.contributors = data;
        },
        function(err) {
          console.log("error occurred when loading contributors: " + JSON.stringify(e));
        });
    }

    $scope.getContributorTitle = function(c) {
      return c.name;
    };

    $scope.getContributorSelectedTitle = function(items) {
      if (!items || items.length === 0) {
        return 'None Selected';
      }
      return items.length + ' Selected';
    };

    $scope.getCollectionTitle = function(c) {
      return c.name.replace('CoreSpring', '');
    };

    $scope.getTitle = function(o) {
      return o.key.replace(/^0/, '');
    };

    $scope.getLabel = function(o) {
      return o.label;
    };

    $scope.getCollectionSelectedTitle = function(items) {
      if (!items || items.length === 0) {
        return 'None Selected';
      }
      return items.length + ' selected';
    };

    $scope.getSelectedTitle = function(items) {
      if (!items || items.length === 0) {
        return 'None Selected';
      }
      var out = _.pluck(items, 'key').map(function(key) {
        var numericKey = parseInt(key);
        return isNaN(numericKey) ? key : numericKey;
      });
      return out.join(', ');
    };

    init();
  }

  SubNavController.$inject = ['$scope', '$rootScope','CollectionManager', 'Contributor'];

  root.tagger = root.tagger || {};
  root.tagger.SubNavController = SubNavController;
})(this);