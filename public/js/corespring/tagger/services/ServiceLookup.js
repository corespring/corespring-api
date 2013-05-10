
/**
 * Lookup for all ajax services.
 */
//window.servicesModule
angular.module('tagger.services')
    .factory('ServiceLookup', function () {




        var ServiceLookup = function () {

            var checkInjectedRoutes = function(injectedRoutes, methodName, params, defaultValue){

              if(!window[injectedRoutes]){
                return defaultValue;
              }

              var routesObject = window[injectedRoutes];

              if(!routesObject[methodName]){
                return defaultValue;
              }

              var result = routesObject[methodName].apply(null, params);
              if(!result || !result.url){
                return defaultValue;
              }
              //The play js functions sometimes add a ? for a query function - strip it here.
              if(result.url.indexOf("?") != -1){
                return result.url.substring(0, result.url.indexOf("?"));
              } else {
                return result.url;
              }
            };

            this.services = {
                //TODO: Do we need method here too? eg POST/PUT
                //TODO: For our keys we sometimes use : and sometimes {}?
                materials: '/api/v1/items/:itemId/materials',
                //uploadSupportingMaterial: '/api/vi/items/{itemId}/materials?name={name}&filename="{filename}',
                uploadSupportingMaterial: '/api/v1/items/{itemId}/materialsWithFile/{name}/{filename}',

                createSupportingMaterial: '/api/v1/items/{itemId}/materials',
                createSupportingMaterialFile: '/api/v1/items/{itemId}/materials/{resourceName}',
                deleteSupportingMaterialFile:'/api/v1/items/{itemId}/materials/{resourceName}/{filename}',
                updateSupportingMaterialFile: '/api/v1/items/{itemId}/materials/{resourceName}/{filename}',
                uploadSupportingMaterialFile:'/api/v1/items/{itemId}/materials/{resourceName}/{filename}/upload',

                items: checkInjectedRoutes('PlayerItemRoutes', 'list', [], '/api/v1/items/:id'),
                itemList: checkInjectedRoutes('PlayerItemRoutes', 'list', [], '/api/v1/items/:id'),
                itemDetails: checkInjectedRoutes('PlayerItemRoutes', 'getDetail', [':id'], '/api/v1/items/:id/detail'),
                itemIncrement:'/api/v1/items/:id/increment',
                getAccessToken:'/web/access_token',

                previewFile:'/web/show-resource/{key}',

                renderResource:'/web/show-resource/{key}/main',
                printResource:'/web/print-resource/{key}/main',

                createDataFile: '/api/v1/items/{itemId}/data',
                deleteDataFile: '/api/v1/items/{itemId}/data/{filename}',
                updateDataFile: '/api/v1/items/{itemId}/data/{filename}',
                uploadDataFile: '/api/v1/items/{itemId}/data/{filename}/upload',

                standardsTree:'/assets/web/standards_tree.json',
                standards:'/api/v1/field_values/cc-standard',
                subject:'/api/v1/field_values/subject',
                collection: checkInjectedRoutes('PlayerCollectionRoutes', 'list', [],'/api/v1/collections'),
                contributor:'/api/v1/contributors',
                uploadFile:'/tagger/upload/{itemId}/{fileName}',
                viewFile:'/tagger/files/{itemId}/{fileName}',
                deleteFile:'/tagger/delete/{itemId}/{fileName}',

                playerPreview: checkInjectedRoutes('PlayerRoutes','preview', [':itemId'], '/player/item/:itemId/preview')
            };
        };

        ServiceLookup.CREATE_SUPPORTING_MATERIAL = "createSupportingMaterial";

        ServiceLookup.prototype.getUrlFor = function (name, substitutions) {
            if (this.services.hasOwnProperty(name)) {

                var template = this.services[name];

                if( substitutions ){
                    for( var x in substitutions){
                        template = template.replace("{" + x + "}", substitutions[x]);
                    }
                }

                return template;
            }
            throw "Can't find service for name: " + name;
        };

        return new ServiceLookup();
    });


