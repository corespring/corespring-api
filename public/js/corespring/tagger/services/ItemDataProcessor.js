window.com = (window.com || {});
com.corespring = (com.corespring || {});
com.corespring.model = (com.corespring.model || {});

com.corespring.model.Defaults = function(){
    
    this.$defaults = (function(){

        if(!window.fieldValues){
          return {};
        }

        return {
          keySkills:_.map(window.fieldValues.keySkills, function (k) {
              return {header:k.key, list:k.value};
          }),
          reviewsPassed:_.map(window.fieldValues.reviewsPassed, function (g) {
              return {key:g.key, label:g.value};
          }),
          gradeLevels:_.map(window.fieldValues.gradeLevels, function (g) {
              return {key:g.key, label:g.value};
          })
        };
    }());


    this.buildNgDataProvider = function(name){

     var _buildNgDataProvider = function (defaults) {

        var out = [];
        for (var x = 0; x < defaults.length; x++) {
            var definition = defaults[x];
            var item = {state:{}};
            angular.extend(item, definition);
            out.push(item);
        }
        return out;
      };

      if(this.$defaults[name]){
          return _buildNgDataProvider(this.$defaults[name]);
      } else {
          return [];
      }
    };

};
/**
 * Manipulates the incoming and outgoing data.
 * Incoming - so it works for the controller.
 * Outgoing - so any extraneaous data is removed and and updates made by the user
 * are in the correct model format for saving.
 * @constructor
 */
com.corespring.model.ItemDataProcessor = function () {


    /**
     * Create Data transfer object to send across the wire.
     * @param itemData - the item data model
     */
    this.createDTO = function (itemData) {
        var dto = {};
        var processedData = { _id:undefined };
        processedData.priorGradeLevel = this.buildDtoKeyArray(itemData.$priorGradeLevelDataProvider);
        processedData.gradeLevel = this.buildDtoKeyArray(itemData.$gradeLevelDataProvider);
        processedData.reviewsPassed = this.buildDtoKeyArray(itemData.$reviewsPassedDataProvider);
        processedData.primarySubject = this.convertEmbeddedToOid(itemData.primarySubject);
        processedData.relatedSubject = this.convertEmbeddedToOid(itemData.relatedSubject);
        processedData.standards = _.map(itemData.standards, function(s){ return s.dotNotation; } );
        processedData.costForResource = parseInt(itemData.costForResource);
        angular.extend(dto, itemData, processedData);
        dto.id = null;
        return dto;
    };

    this.convertEmbeddedToOid = function (item) {
        if (!item || !item.id) {
            return null;
        }
        return  item.id;
    }

    /**
     *
     * @param dataProvider - the ng style dataprovider:
     * [ {state: { selected: true, key: "blah", label: "blah" }}, ...]
     * @return {Array} of values that were true eg: ["Blah"]
     */
    this.buildDtoKeyArray = function (dataProvider) {

        var out = [];
        for (var i = 0; i < dataProvider.length; i++) {
            var def = dataProvider[i];
            if (def.state.selected) {
                out.push(def.state.key);
            }
        }
        return out;
    };


    /**
     * Before we return the resource to the controller,
     * build out the object structure. Also map any data that needs to be mapped
     * to allow the controller and bindings to function.
     * @param resource
     */
    this.processIncomingData = function (item) {

        item.$defaults = this.$defaults;

        if (item.files == null) {
            item.files = [];
        }

        if (item.standards == null) {
            item.standards = [];
        }

        if (item.primaryStandard != null) {
            throw "Data model contains a 'primaryStandard' - need to move these to the 'standards' array";
        }

        if (!item.workflow) {
            item.workflow = this.createWorkflowObject();
        }


        function getKey(o){ return o.key }

        /**
         * Dataproviders are prepended with a $ so that angular doesn't send them across the wire.
         * @type {Array}
         */
        item.$gradeLevelDataProvider = this.buildNgDataProvider(this.$defaults.gradeLevels, item.gradeLevel);
        item.$priorGradeLevelDataProvider = this.buildNgDataProvider(this.$defaults.gradeLevels, item.priorGradeLevel);
        item.$reviewsPassedDataProvider = this.buildNgDataProvider(this.$defaults.reviewsPassed, item.reviewsPassed);
        item.$priorUseDataProvider = _.map(window.fieldValues.priorUses, getKey);
        item.$credentialsDataProvider = _.map(window.fieldValues.credentials, getKey);
        item.$licenseTypeDataProvider = _.map(window.fieldValues.licenseTypes, getKey);
        item.$bloomsTaxonomyDataProvider = _.map(window.fieldValues.bloomsTaxonomy, getKey);
        item.$itemTypeDataProvider = _.filter( window.fieldValues.itemTypes, function(c){ return c.value != "Other" });
        item.$demonstratedKnowledgeDataProvider = _.map(window.fieldValues.demonstratedKnowledge, getKey);

        if (!item.keySkills) {
            item.keySkills = [];
        }
    };

    this.$defaults = (function(){

        if(!window.fieldValues){
          return {};
        }

        return {
          keySkills:_.map(window.fieldValues.keySkills, function (k) {
              return {header:k.key, list:k.value}
          }),
          reviewsPassed:_.map(window.fieldValues.reviewsPassed, function (g) {
              return {key:g.key, label:g.value}
          }),
          gradeLevels:_.map(window.fieldValues.gradeLevels, function (g) {
              return {key:g.key, label:g.value}
          })
        }
    }());

    this.createWorkflowObject = function () {
        return {
            setup:false,
            tagged:false,
            standardsAligned:false,
            qaReview:false
        };
    };

    /**
     * Take a simple array of values like so: ['A','B','C',...]
     * and convert it to: { {state:{ key: A, selected: true, label: x}}, ... }
     *
     * This object format is necessary if you with to build repeaters off the datamodel.
     * @see https://github.com/angular/angular.js/issues/686
     *
     * This may change or be fixed which will allow this to be simplified.
     * @param defaults - an array of defaults that drive the ui
     * @param dtoArray - the values saved in the datamodel
     * @return {Array}
     */
    this.buildNgDataProvider = function (defaults, dtoArray) {

        function getValueFromModel(key, dtoArray) {

            if (dtoArray === undefined) {
                return false;
            }

            /**
             * @deprecated
             * Capture old data model that has moved from a single string value to an
             * array of string values.
             */
            if (typeof(dtoArray) == "string") {
                console.warn("deprecated: Captured simple string value");
                return key === dtoArray;
            }

            if (dtoArray.indexOf !== undefined) {
                var index = dtoArray.indexOf(key);
                return index != -1;
            }
            else {
                /**
                 * @deprecated - this is to support a legacy data format - to be removed
                 */
                console.warn("deprecated: using an old data model format");
                var item = (dtoArray[key] || dtoArray[parseFloat(key)]);
                return item;
            }
            return false;
        }

        var out = [];
        for (var x = 0; x < defaults.length; x++) {
            var definition = defaults[x];
            var selected = getValueFromModel(definition.key, dtoArray);
            var item = {state:{}};
            angular.extend(item.state, definition, {selected:selected});
            out.push(item);
        }
        return out;
    };

    this.buildObjectFromTokens = function (tokens, initialValue) {
        var out = {};
        var tokensArray = tokens.split(",");
        for (var i = 0; i < tokensArray.length; i++) {
            out[ tokensArray[i] ] = initialValue;
        }
        return out;
    };
}
;
