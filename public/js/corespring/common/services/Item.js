angular.module('corespring-services', [])
  .factory('Item',
    [ '$resource',
      function ($resource) {

        var api = PlayerItemRoutes;
        var calls  = {
          read: api.read(":itemId")
        };
        console.log(api);
        return $resource(
          calls.read.url,
          {},
          {
            read: calls.read
          }
        );
      }
    ]);
