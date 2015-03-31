(function(root) {

  function HomeController($scope,
    $timeout,
    $rootScope,
    $http,
    $location,
    ItemService,
    SearchService,
    CollectionManager,
    Contributor,
    ItemFormattingUtils,
    Logger,
    CmsService,
    UserInfo,
    DraftItemService) {

    //Mixin ItemFormattingUtils
    angular.extend($scope, ItemFormattingUtils);


    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";


    $rootScope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();
    $rootScope.$broadcast('onListViewOpened');

    var init = function() {
      $scope.userName = UserInfo.userName;
      $scope.org = UserInfo.org;
      loadDraftsForOrg();
      loadCollections();
      loadContributors();
      $scope.showDraft = true;

      var defaultsFactory = new com.corespring.model.Defaults();
      $scope.gradeLevelDataProvider = defaultsFactory.buildNgDataProvider("gradeLevels");
      $scope.itemTypeDataProvider = defaultsFactory.buildNgDataProvider("itemTypes");
      $scope.flatItemTypeDataProvided = _.map(_.flatten(_.pluck($scope.itemTypeDataProvider, 'label')), function(e) {
        return {
          key: e,
          label: e
        };
      });
      $scope.flatItemTypeDataProvided.push({
        key: "Other",
        label: "Other"
      });
      $scope.statuses = [{
        label: "Setup",
        key: "setup"
      }, {
        label: "Tagged",
        key: "tagged"
      }, {
        label: "Standards Aligned",
        key: "standardsAligned"
      }, {
        label: "QA Review",
        key: "qaReview"
      }, {
        label: "Exact Match",
        key: "exactMatch"
      }];
      $scope.publishStatuses = [{
        label: "Published",
        key: "published"
      }, {
        label: "Draft",
        key: "draft"
      }];
    };

    $scope.editItem=function(item){
      console.log('editItem', item);
      if(!item.readOnly){
        $scope.itemClick.bind({item: item})();
      }
    };

    $scope.viewItem = function(item){
      if(item.readOnly){
        $scope.itemClick.bind(item)();
      }
    };

    $scope.goLive = function(item){
      $scope.$emit('goLiveRequested',item);
    };

    $scope.deleteItem = function(item) {
      $scope.itemToDelete = item;
      $scope.showConfirmDestroyModal = true;
    };

    $scope.deleteConfirmed = function() {
      var deletingId = $scope.itemToDelete.id;
      ItemService.remove({
          id: $scope.itemToDelete.id
        },
        function(result) {
          $scope.itemToDelete = null;
          $scope.search();
        }
      );
      $scope.itemToDelete = null;
      $scope.showConfirmDestroyModal = false;
    };

    $scope.deleteCancelled = function() {
      $scope.itemToDelete = null;
      $scope.showConfirmDestroyModal = false;
    };

    $scope.makeADraft = function(item){

      CmsService.itemFormat('item', item.id, function(format) {

        if(format.apiVersion !== 2){
          alert('Drafts are not supported for v1 items, format: ' + JSON.stringify(format));
          return;
        }

        item.createUserDraft(function(draft){
          console.debug('draft', draft);
          goToEditDraft(draft.id, item);
          }, function error(err){
          alert('error making a draft' + JSON.stringify(err));
        });
      });

    };

    $scope.editDraft  = function(draft){
      goToEditDraft(draft.id);
    };

    $scope.deleteDraft = function(draft){
      DraftItemService.deleteDraft(draft.id, function(result){
        console.log('deleting draft, successful');
        $scope.orgDrafts = _.reject($scope.orgDrafts, function(d){
          return d.id === draft.id;
        });
      }, function(err){
        console.warn('Error deleting draft');
      });
    };

    function goToEditDraft(draftId, item){
      CmsService.itemFormat('draft', draftId, function(format) {
        Logger.debug('itemFormat:', format);
        SearchService.currentItem = item;
        if (format.apiVersion === 2) {
          $location.url('/edit/draft/' + draftId);
        } else {
          throw new Error('editing v1 drafts not ready yet.');
        }
      });
    }

    $scope.cloneItem = function(item){
      item.clone(function success(data){
        goToEditView(data);
      }, function error(err){
        alert('cloneItem:', JSON.stringify(err));
      });
    };

    $scope.getNumberOfSessions = function(id){
      //TODO: Are we going to add this?
    };

    $scope.sortBy = function(field) {
      if ($rootScope.searchParams.sort && $rootScope.searchParams.sort[field]) {
        $rootScope.searchParams.sort[field] *= -1;
      } else {
        $rootScope.searchParams.sort = {};
        $rootScope.searchParams.sort[field] = 1;
      }
      $scope.$broadcast("sortingOnField", field, $rootScope.searchParams.sort[field] == 1);
      $scope.search();
    };

    $scope.getContributorTitle = function(c) {
      return c.name;
    };

    $scope.getContributorSelectedTitle = function(items) {
      if (!items || items.length === 0) {
        return "None Selected";
      }
      return items.length + " Selected";
    };

    $scope.getCollectionTitle = function(c) {
      return c.name.replace("CoreSpring", "");
    };

    $scope.getTitle = function(o) {
      return o.key.replace(/^0/, "");
    };
    $scope.getLabel = function(o) {
      return o.label;
    };

    $scope.getCollectionSelectedTitle = function(items) {
      if (!items || items.length === 0) {
        return "None Selected";
      }
      return items.length + " selected";
    };

    $scope.getSelectedTitle = function(items) {
      if (!items || items.length === 0) {
        return "None Selected";
      }
      var out = _.pluck(items, "key").map(function(key) {
        var numericKey = parseInt(key);
        return isNaN(numericKey) ? key : numericKey;
      });
      return out.join(", ");
    };

    function applyPermissions(items) {
      var readOnlyCollections = _.filter(CollectionManager.rawCollections, function(c) {
        return c.permission == "read";
      });
      return _.map(items, function(item) {
        var readOnlyColl = _.find(readOnlyCollections, function(coll) {
          return coll.id == item.collectionId; //1 represents read-only access
        });
        if (readOnlyColl) {
          item.readOnly = true;
        } else {
          item.readOnly = false;
        }
        return item;
      });
    }

    $scope.lazySearch = _.debounce(function() {
      $scope.search();
      $scope.$apply();
    }, 500);

    $scope.search = function() {
      var isOtherSelected = $rootScope.searchParams && _.find($rootScope.searchParams.itemType, function(e) {
        return e.label == "Other";
      });

      if (isOtherSelected) {
        $rootScope.searchParams.notSelectedItemTypes = [];
        _.each($scope.flatItemTypeDataProvided, function(e) {
          var isSelected = _.find($rootScope.searchParams.itemType, function(f) {
            return e.label == f.label;
          });
          if (!isSelected)
            $rootScope.searchParams.notSelectedItemTypes.push(e);
        });
      }
      SearchService.search($rootScope.searchParams, function(res) {
        $rootScope.items = applyPermissions(res);
        setTimeout(function() {
          MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }, 200);
      });
    };

    $scope.loadMore = function() {
      SearchService.loadMore(function() {
        // re-bind the scope collection to the services model after result comes back
        $rootScope.items = applyPermissions(SearchService.itemDataCollection);
        //Trigger MathJax
        setTimeout(function() {
          MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }, 200);

      });
    };

    function loadDraftsForOrg(){
      CmsService.getDraftsForOrg(function(drafts){
        $scope.orgDrafts = drafts;
      }, function error(err){
        console.warn('error: getDraftsForOrg', err);
      });
    }

    function loadCollections() {

      $scope.$watch(function() {
        return CollectionManager.sortedCollections;
      }, function(newValue, oldValue) {
        if (newValue) {
          $scope.sortedCollections = newValue;
          if (!$rootScope.searchParams.collection && $scope.sortedCollections) {
            $rootScope.searchParams.collection = _.clone($scope.sortedCollections[0].collections);
          }
          $scope.search();
        }
      }, true);

      CollectionManager.init();
    }

    function loadContributors() {
      Contributor.get({}, function(data) {
          $scope.contributors = data;
        },
        function(err) {
          console.log("error occurred when loading contributors: " + JSON.stringify(e));
        });
    }

    $scope.showGradeLevel = function() {
      return $scope.createGradeLevelString(this.item.gradeLevel);
    };
  

    function goToEditView(item){
      CmsService.itemFormat('item', item.id, function(format) {
        Logger.debug('itemFormat:', format);
        SearchService.currentItem = item;
        if (format.apiVersion === 2) {
          $scope.search();
          Logger.warn('can\'t directly edit a v2 item - you need to create a draft');
        } else {
          $location.url('/old/edit/' + item.id + "?panel=metadata");
        }
      });
    }

    $scope.launchCatalogView = function(){
      $scope.openItem(this.item.id);
    };

    $scope.openItem = function(id) {

      $timeout(function() {
        $scope.showPopup = true;
        $scope.popupBg = "extra-large-window";
        $scope.previewingId = id;
        $('#preloader').show();
        $('#player').hide();
      }, 50);
      $timeout(function() {
        $('.window-overlay').scrollTop(0);
      }, 100);
    };

    $scope.itemClick = function() {
      var itemId = this.item.id;

      if (this.item.readOnly) {
        $scope.openItem(itemId);
      } else {
        goToEditView(this.item);
      }
    };

    $scope.publishStatus = function(isPublished) {
      if (isPublished) return "Published";
      else return "Draft";
    };

    //from items-app.js
    $scope.hidePopup = function() {
      $scope.showPopup = false;
      $scope.previewingId = "";
      $scope.popupBg = "";
    };


    $scope.onItemLoad = function() {
      $('#preloader').hide();
      $('#player').show();
    };

    var fn = function(m) {
      try {
        var data = JSON.parse(m.data);
        if (data.message == 'closeProfilePopup') {
          $timeout(function() {
            $scope.hidePopup();
          }, 10);
        }
      } catch (err) {
        //it is normal for this error to be thrown
        // Logger.error("Error occurred on window event listener in home controller: "+JSON.stringify(err));
      }
    };

    if (window.addEventListener) {
      window.addEventListener('message', fn, true);
    } else if (window.attachEvent) {
      window.attachEvent('message', fn);
    }

    init();
  }

  HomeController.$inject = ['$scope',
    '$timeout',
    '$rootScope',
    '$http',
    '$location',
    'ItemService',
    'SearchService',
    'CollectionManager',
    'Contributor',
    'ItemFormattingUtils',
    'Logger',
    'CmsService',
    'UserInfo',
    'DraftItemService'
  ];

  root.tagger = root.tagger || {};
  root.tagger.HomeController = HomeController;

})(this);