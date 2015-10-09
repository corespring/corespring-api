angular.module('tagger.services')
  .service('ItemDraftService', ['$http', function($http) {

    function ItemDraftService() {

      this.publish = function(id, onSuccess, onError) {
        throw new Error('publish on drafts not supported');
      };

      this.clone = function(id, onSuccess, onError) {
        var url = '/api/v2/items/drafts/' + id + '/clone';

        $http.put(url)
          .success(onSuccess)
          .error(onError);
      };

      this.get = function(params, onSuccess, onError) {

        var url = '/api/v2/items/drafts/' + params.id;

        if (params.ignoreConflict) {
          url += '?ignoreConflicts=true';
        }

        $http.get(url)
          .success(onSuccess)
          .error(onError);
      };

      this.deleteDraft = function(id, onSuccess, onError, all) {
        var url = '/api/v2/items/drafts/' + id;

        if (all) {
          url += '?all=true';
        }

        $http['delete'](url)
          .success(onSuccess)
          .error(onError);
      };

      this.deleteByItemId = function(id, onSuccess, onError) {
        this.deleteDraft(id, onSuccess, onError, true);
      };

      this.commit = function(id, force, onSuccess, onError) {

        force = force === true;

        var url = '/api/v2/items/drafts/' + id + '/commit';

        if (force) {
          url += '?force=true';
        }

        $http.put(url)
          .success(onSuccess)
          .error(onError);
      };

      this.createUserDraft = function(itemId, onSuccess, onError) {
        var listUrl = '/api/v2/items/' + itemId + '/drafts';
        var createUrl = '/api/v2/items/' + itemId + '/draft';

        $http.get(listUrl)
          .success(function(drafts) {
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

