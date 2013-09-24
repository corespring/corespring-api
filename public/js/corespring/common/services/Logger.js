function loadModule(name) {
  try {
    return angular.module(name);
  }
  catch (e) {
    return angular.module(name, []);
  }
}

loadModule('corespring-services').factory('Logger', ['$resource', function($resource){

    var logger = $resource(
        '/logger/:logType',
        {},
        {
            fatal: {method: 'POST', params: {logType: 'fatal'}},
            error: {method: 'POST', params: {logType: 'error'}},
            warn: {method: 'POST', params: {logType: 'warn'}},
            info: {method: 'POST', params: {logType: 'info'}},
            debug: {method: 'POST', params: {logType: 'debug'}}
        }
    )
    return {
        fatal: function(message){
            logger.fatal({message: message})
        }
    }
}])