angular.module('tagger.services')
  .factory('File',
  ['$resource', 'ServiceLookup',
    function ($resource, ServiceLookup) {
      return $resource(
        ServiceLookup.getUrlFor('file') + '/:filename',
        {},
        {
          upload: {method: 'POST'}
        }
      );
    }
  ]);

angular.module('tagger.services')
  .factory('ExtendedData', [
    '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

      return $resource(
        ServiceLookup.getUrlFor('extendedItemData'),
        {},
        {
          "update": {method: 'PUT'}
        }
      );
    }
  ]);

angular.module('tagger.services')
  .factory('ItemMetadata', [
    '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

      return $resource(
        ServiceLookup.getUrlFor('itemMetadata') + '/:id',
        {},
        {
          "get": {method: 'GET', isArray: true}
        }
      );
    }
  ]);

angular.module('tagger.services')
  .factory('ItemSessionCountService', [
    '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

      return $resource(
        ServiceLookup.getUrlFor('items') + '/sessions/count',
        {},
        {
          "get": {method: 'GET'}
        }
      );
    }
  ]);

angular.module('tagger.services')
  .factory('SupportingMaterial',
  ['$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

    return $resource(
      ServiceLookup.getUrlFor('materials') + '/:resourceName',
      {},
      {
        query: {method: 'GET', isArray: true},
        "delete": {method: 'DELETE', isArray: false}
      }
    );
  }]);

angular.module('tagger.services')
  .factory('V2ItemService', ['$http',
    function ($http) {

      function V2ItemService() {
        this.create = function (data, onSuccess, onError) {
          $http.post('/api/v2/items', data)
            .success(onSuccess)
            .error(onError);
        };

        this.publish = function (params, onSuccess, onError) {
          var url = "/api/v2/items/:id/publish".replace(":id", params.id);

          $http.put(url, {})
            .success(onSuccess)
            .error(onError);
        };

        this.delete = function( data, onSuccess, onError ){
          $http.delete('/api/v2/items', data)
            .success(onSuccess)
            .error(onError);
        };

        this.clone = function (params, onSuccess, onError) {
          var url = "/api/v2/items/:id/clone".replace(":id", params.id);

          $http.post(url, {})
            .success(onSuccess)
            .error(onError);
        };

      }

      return new V2ItemService();


    }]);

angular.module('tagger.services')
  .service('ItemDraftService', ['$http', function ($http) {

    function ItemDraftService() {

      this.publish = function (id, onSuccess, onError) {
        throw new Error('publish on drafts not supported');
      };

      this.clone = function (id, onSuccess, onError) {
        var url = '/api/v2/items/drafts/' + id + '/clone';

        $http.put(url)
          .success(onSuccess)
          .error(onError);
      };

      this.get = function (params, onSuccess, onError) {

        var url = '/api/v2/items/drafts/' + params.id;

        if (params.ignoreConflict) {
          url += '?ignoreConflicts=true';
        }

        $http.get(url)
          .success(onSuccess)
          .error(onError);
      };

      this.deleteDraft = function (id, onSuccess, onError, all) {
        var url = '/api/v2/items/drafts/' + id;

        if (all) {
          url += '?all=true';
        }

        $http['delete'](url)
          .success(onSuccess)
          .error(onError);
      };

      this.deleteByItemId = function (id, onSuccess, onError) {
        this.deleteDraft(id, onSuccess, onError, true);
      };

      this.commit = function (id, force, onSuccess, onError) {

        force = force === true;

        var url = '/api/v2/items/drafts/' + id + '/commit';

        if (force) {
          url += '?force=true';
        }

        $http.put(url)
          .success(onSuccess)
          .error(onError);
      };

      this.createUserDraft = function (itemId, onSuccess, onError) {
        var listUrl = '/api/v2/items/' + itemId + '/drafts';
        var createUrl = '/api/v2/items/' + itemId + '/draft';

        $http.get(listUrl)
          .success(function (drafts) {
            if (drafts.length === 0) {
              $http.post(createUrl)
                .success(onSuccess)
                .error(onError);
            } else {
              onError({msg: 'There is already a draft for this item'});
            }
          })
          .error(onError);
      };

    }

    return new ItemDraftService();

  }]);

angular.module('tagger.services')
  .factory('ItemService', [
    '$resource',
    'ServiceLookup',
    '$http',
    'V2ItemService',
    'ItemDraftService',
    function ($resource,
              ServiceLookup,
              $http,
              V2ItemService,
              ItemDraftService) {

      var ItemService = $resource(
        ServiceLookup.getUrlFor('items'),
        {},
        {
          update: {method: 'PUT'},
          query: {method: 'GET', isArray: true},
          count: {method: 'GET', isArray: false}
        }
      );

      ItemService.prototype.saveNewVersion = function (onSuccess, onError) {
        var url = "/api/v2/items/:id/save-new-version".replace(":id", this.id);
        $http.put(url, {})
          .success(onSuccess)
          .error(onError);
      };

      ItemService.prototype.publish = function (onSuccess, onError, id) {
        id = id || this.id;
        var url = "/api/v2/items/:id/publish".replace(":id", id);
        $http.put(url, {})
          .success(onSuccess)
          .error(onError);
      };

      ItemService.prototype.clone = function (onSuccess, onError) {
        V2ItemService.clone(this, onSuccess, onError);
      };

      ItemService.prototype.update = function (paramsObject, cb, onErrorCallback) {
        var idObject = angular.extend(paramsObject, {id: this.id});

        var dto = ItemService.processor.createDTO(this);
        return ItemService.update(idObject, dto, function (resource) {
          ItemService.processor.processIncomingData(resource);
          cb(resource);
        }, onErrorCallback);
      };


      ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id: this.id}, cb);
      };

      ItemService.prototype.createUserDraft = function (onSuccess, onError) {
        ItemDraftService.createUserDraft(this.id, onSuccess, onError);
      };

      ItemService.processor = new com.corespring.model.ItemDataProcessor();
      ItemService.createWorkflowObject = ItemService.processor.createWorkflowObject;


      //The currently retrieved item
      ItemService._currentItemService = null;
      //The current Item id
      ItemService._currentItemId = null;
      //stash the default get implementation
      ItemService.angularGet = ItemService.get;
      //ItemService.angularUpdate = ItemService.update;

      ItemService._getInProgress = false;

      ItemService._getCallbacks = {};

      ItemService.resourceLoaded = function (callback, resource) {
        ItemService._curentItemId = resource.id;
        ItemService._getInProgress = false;
        ItemService.processor.processIncomingData(resource);
        ItemService._currentItemService = new ItemService(resource);
        callback(ItemService._currentItemService);

        var pendingCallbacks = ItemService._getCallbacks[ItemService._curentItemId];

        if (pendingCallbacks === undefined) {
          return;
        }
        _.forEach(pendingCallbacks, function (pc) {
          pc(ItemService._currentItemService);
        });

      };

      /**
       * Several controllers within the edit context would like the itemData object.
       * The service keeps an instance stored - and if the controller asks for the same item
       * as is currently loaded it returns it.
       * @param object
       * @param callback
       */
      ItemService.get = function (object, callback) {

        if (ItemService._getInProgress) {

          if (ItemService._getCallbacks[object.id] == undefined) {
            ItemService._getCallbacks[object.id] = [];
          }
          ItemService._getCallbacks[object.id].push(callback);
          return;
        }
        if (object.id === ItemService._currentItemId) {
          callback(ItemService._currentItemService);
        }
        else {
          ItemService._getCallbacks = {};
          ItemService._currentItemId = null;
          ItemService._getInProgress = true;
          ItemService.angularGet(object, function (resource) {
            ItemService.resourceLoaded(callback, resource)
          });
        }
      };

      ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id: this._id.$oid}, cb);
      };

      return ItemService;
    }]
);
