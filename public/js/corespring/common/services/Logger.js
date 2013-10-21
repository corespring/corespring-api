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
        fatal: function(message,stacktrace){
            if(stacktrace){
                logger.fatal({message: message, stacktrace: stacktrace.toString()});
            }else{
                var trace = printStackTrace();
                trace.splice(0,5); //offset to compensate for inclusion of stacktrace.js calls and Logger.js calls within the trace
                logger.fatal({message: message, stacktrace: trace.join("\n")});
            }
        },
        error: function(message){
            var trace = printStackTrace();
            trace.splice(0,5); //offset to compensate for inclusion of stacktrace.js calls and Logger.js calls within the trace
            logger.error({message: message, stacktrace: trace.join("\n")});
        },
        warn: function(message){
            logger.warn({message: message});
        },
        info: function(message){
            logger.info({message: message});
        },
        debug: function(message){
            logger.debug({message: message});
        }
    }
}])
loadModule('corespring-servicse').factory('$exceptionHandler', ['Logger', function (Logger) {
    return function (exception, cause) {
        Logger.fatal("Angular exception thrown: "+exception);
    };
}]);