/** Encapsulates Collection data and behaviour.
 * It will perform all the necessary updates to the 'sortedCollection' to the only thing a client needs to do is to $watch the sorted collection:
 * EG:
 * {{{
 *  $scope.$watch(
 *  function () {
 *     return CollectionManager.sortedCollections;
 *   },
 * function (newValue) {
 *     console.log("CreateCollection -> new sortedCollections: " + newValue);
 *    if (newValue) {
 *      $scope.collections = newValue[0].collections;
 *     }
 *   }, true);
 * }}}
 * TODO: We do alot of data formatting on the client side here - should that be server side instead?
 */
angular.module('tagger.services')
  .factory('CollectionManager', [ 'Collection', 'UserInfo', 'Logger', 'CollectionEnabledStatus', 'ShareCollection', function (Collection, UserInfo, Logger, CollectionEnabledStatus, ShareCollection) {
    "use strict";

    var rawData = {};


    var getCollectionById = function (id) {

      if (out.sortedCollections[0]) {
        return _.find(out.sortedCollections[0].collections, function (c) {
          return c.id === id;
        });
      }
    };


    var updateNameSortedCollection = function (newCollection) {
      if (out.sortedCollections[0]) {
        out.sortedCollections[0].collections = _.map(out.sortedCollections[0].collections, function (c) {
          if (c.id === newCollection.id) {
            c.name = newCollection.name;
          }
          return c;
        });
      }
    };



    var removeCollectionFromSortedCollection = function (id) {
      if (out.sortedCollections[0]) {
        out.sortedCollections[0].collections = _.filter(out.sortedCollections[0].collections, function (c) {
          return c.id !== id;
        });
      }
    };

    var addNewCollectionToSortedCollection = function (newCollection) {
      if (out.sortedCollections[0]) {
        out.sortedCollections[0].collections.push(newCollection);
      }
    }

    var createSortedCollection = function (collections, userOrg) {
      if (!collections || !userOrg) {
        return [];
      }

      var getName = function (id) {
        var out = _.find(collections, function (c) {
          return c.id === id;
        });
        return out ? out.name : "?";
      };

      var isOwner = function (c) {
        return userOrg.id === c.ownerOrgId;
      };

      var convertProperties = function (c) {
        return { id: c.collectionId, name: getName(c.collectionId), permission: c.permission, enabled: c.enabled };
      };

      var getItemCount = function (id) {
        var collection = _.find(collections, function (c) {
          return c.id === id;
        });
        return collection ? collection.itemCount : 0;
      };

      var addItemCount = function (c) {
        c.itemCount = getItemCount(c.id);
        return c;
      };

      var notInUserOrgs = function (c) {
        return userIds.indexOf(c.id) === -1;
      };

      var addEnabledState = function (c) {
        c.enabled = getEnabledState(c.id);
        return c;
      };

      var getEnabledState = function (id) {
        var collection = _.find(userOrg.collections, function (c) {
          return c.collectionId === id;
        });
        return collection ? collection.enabled : false;
      };



      collections = _.map(collections, addEnabledState);
      userOrg.collections = _.filter(collections, isOwner);

      var sharedCollections = {
        name: "Shared",
        collections: _.filter(collections, function(c){
          return c.ownerOrgId != userOrg.id;
        })
      };

      var userIds = _.pluck(userOrg.collections, "id");

      delete userOrg.path;
      delete userOrg.id;

      // return colls for this org and colls shared with the org
      return [userOrg, sharedCollections];
    };

    var initialize = function (onComplete) {
      Collection.get({}, function (data) {
          rawData.collection = data;
          out.rawCollections = data;
          out.sortedCollections = createSortedCollection(data, _.clone(UserInfo.org));
          if (onComplete) onComplete();
        },
        function (err) {
            Logger.error("Error initializing collections: "+JSON.stringify(err));
        });
    };


    var updateCollectionEnabledStatus = function(collectionId, status, onSuccess, onError) {
      CollectionEnabledStatus.update({id: collectionId, enabled : status }, {}, onSuccess, onError);

      if (out.sortedCollections[1]) {
        out.sortedCollections[1].collections = _.map(out.sortedCollections[1].collections, function (c) {
          if (c.id === collectionId) {
            c.enabled = status;
          }
          return c;
        });
      }

    };

    /** PUBLIC */
    var out = {
      /** Add a new collection */
      addCollection: function (name, onSuccess, onError) {
        var param = {name: name};
        var successHandler = function (data) {
          rawData.collection.push(data);
          addNewCollectionToSortedCollection(data);
          if (onSuccess) {
            onSuccess(data);
          }
        };
        Collection.create({}, param, successHandler, onError);
      },
      /** remove a collection by id */
      removeCollection: function (id, onSuccess, onError) {
        var successHandler = function () {
          removeCollectionFromSortedCollection(id);
          if (onSuccess) {
            onSuccess(id);
          }
        };
        Collection.remove({id: id}, successHandler, onError);
      },
      /** rename an existing collection */
      renameCollection: function (id, newName, onSuccess, onError, onNoChange) {


        var collection = getCollectionById(id);
        if (collection.name == newName) {
          onNoChange();
          return;
        }
        var successHandler = function (data) {
          updateNameSortedCollection(data);
          if (onSuccess) {
            onSuccess(data);
          }
        };

        Collection.update({id: id}, {name: newName}, successHandler, onError);
      },

      disableCollection: function(collectionId, onSuccess, onError) {
        updateCollectionEnabledStatus(collectionId, false, onSuccess, onError);
      },

      enableCollection: function(collectionId, onSuccess, onError) {
        updateCollectionEnabledStatus(collectionId, true,  onSuccess, onError);
      },

      init: function (onComplete) {
        initialize(onComplete);
      },
      shareCollection: function(collectionId, orgId, onSuccess, onError) {
        ShareCollection.update({id: collectionId, orgId : orgId }, {}, onSuccess, onError);
      },
      rawCollections: null,
      sortedCollections: null
    };
    return out;
  }]);