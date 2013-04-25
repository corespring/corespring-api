function loadModule(name) {
  try {
    return angular.module(name);
  }
  catch (e) {
    return angular.module(name, []);
  }
}

var corespringServiceModule = loadModule('corespring-services');

corespringServiceModule
  .factory('Item',
    [ '$resource',
      function ($resource) {

        var api = PlayerItemRoutes;
        var calls  = {
          read: api.getDetail(":itemId")
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
