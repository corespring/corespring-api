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
  .factory('CollectionManager', [ 'Collection', 'UserInfo', function (Collection, UserInfo) {
    "use strict";

    var rawData = {};

    var updateNameSortedCollection = function (newCollection) {
      if (out.sortedCollections[0]) {
        out.sortedCollections[0].collections = _.map(out.sortedCollections[0].collections, function (c) {
          return c.id === newCollection.id ? newCollection : c;
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

      var hasWritePermission = function (c) {
        return c.permission === "write";
      };

      var toIdAndName = function (c) {
        return { id: c.collectionId, name: getName(c.collectionId), permission: c.permission };
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

      delete userOrg.path;
      delete userOrg.id;
      userOrg.collections = _.filter(userOrg.collections, hasWritePermission);
      userOrg.collections = _.map(userOrg.collections, toIdAndName);
      userOrg.collections = _.map(userOrg.collections, addItemCount);

      var userIds = _.pluck(userOrg.collections, "id");

      var publicCollection = {
        name: "Public",
        collections: _.filter(collections, notInUserOrgs)
      };

      return [userOrg, publicCollection];
    };

    var initialize = function (onComplete) {
      Collection.get({}, function (data) {
          rawData.collection = data;
          out.rawCollections = data;
          out.sortedCollections = createSortedCollection(data, _.clone(UserInfo.org));
          if (onComplete) onComplete();
        },
        function () {
          console.log("load collections: error: " + arguments);
        });
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
      renameCollection: function (id, newName, onSuccess, onError) {

        var successHandler = function (data) {
          updateNameSortedCollection(data);
          if (onSuccess) {
            onSuccess(data);
          }
        };

        Collection.update({id: id}, {name: newName}, successHandler, onError);
      },
      init: function (onComplete) {
        initialize(onComplete);
      },
      rawCollections: null,
      sortedCollections: null
    };
    return out;
  }]);