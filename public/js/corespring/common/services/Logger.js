//use injector instead of http directly because we need to use this in exception hanlder
angular.module('corespring-logger',[]).factory('Logger', ['$injector', '$log', function($injector, $log){

    //everything must go around a try catch block in order to avoid exceptions being thrown and handled by exception handler
    //if exception handler does end up handling things, an infinite loop will occur
    return {
        fatal: function(message,toServer,stacktrace){
            try{
                $log.error(message);
                if(!toServer) return;
                if(stacktrace){
                    $injector.get("$http").post("/logger/fatal",{message: message, stacktrace: stacktrace.toString()})
                }else{
                    var trace = printStackTrace();
                    trace.splice(0,5); //offset to compensate for inclusion of stacktrace.js calls and Logger.js calls within the trace
                    $injector.get("$http").post("/logger/fatal", {message: message, stacktrace: trace.join("\n")});
                }
            } catch (e) {}
        },
        error: function(message,toServer){
            try{
                $log.error(message);
                if(!toServer) return;
                var trace = printStackTrace();
                trace.splice(0,5); //offset to compensate for inclusion of stacktrace.js calls and Logger.js calls within the trace
                $injector.get("$http").post("/logger/error",{message: message, stacktrace: trace.join("\n")});
            } catch (e) {}
        },
        warn: function(message, toServer){
            try{
                $log.warn(message);
                if(!toServer) return;
                $injector.get("$http").post("/logger/warn", {message: message});
            } catch (e) {}
        },
        info: function(message,toServer){
            try{
                $log.info(message);
                if(!toServer) return;
                $injector.get("$http").post("/logger/info", {message: message});
            } catch (e) {}
        },
        debug: function(message,toServer){
            try{
                $log.info(message);
                if(!toServer) return;
                $injector.get("$http").post("/logger/debug", {message: message});
            } catch (e) {}
        }
    }
}])
angular.module('corespring-logger').factory('$exceptionHandler', ['Logger', function (Logger) {
    return function (exception, cause) {
        Logger.fatal("Angular exception thrown: "+exception);
    };
}]);