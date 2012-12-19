angular.module('corespring-utils', []);

angular.module('corespring-utils')
  .factory('ItemFormattingUtils', [function () {

    return {

      getPrimarySubjectLabel: function (primarySubject) {
        if (!primarySubject) {
          return "";
        }
        var out = [];
        if (primarySubject.category) {
          out.push(primarySubject.category);
        }

        if (primarySubject.subject) {
          out.push(primarySubject.subject);
        }
        return out.join(": ");
      },


      createGradeLevelString: function (gradeLevels) {

        function sortGradeLevels(a, b) {
          var orderGuide = "PK,KG,01,02,03,04,05,06,07,08,09,10,11,12,13,PS,AP,UG,Other".split(",");
          var aIndex = orderGuide.indexOf(a);
          var bIndex = orderGuide.indexOf(b);
          if (aIndex == bIndex) {
            return 0;
          }
          return aIndex > bIndex ? 1 : -1;
        }

        if (gradeLevels === undefined) {
          return "";
        }
        var out = [];

        for (var x = 0; x < gradeLevels.length; x++) {
          out.push(gradeLevels[x]);
        }
        out.sort(sortGradeLevels);
        return out.join(",");
      },


      /**
       * Build the standards label:
       * @param standards
       * @return label string
       */
      buildStandardLabel: function (standards) {
        if (standards == null || standards.length == 0) {
          return "";
        }

        var out = standards[0].dotNotation;

        if (standards.length == 1) {
          return out;
        }

        return out + " plus " + (standards.length - 1) + " more";
      },


      buildStandardTooltip: function (standards) {
        if (!standards) {
          return "";
        }
        var out = [];

        if (standards.length == 1 && standards[0].standard) {
          return standards[0].standard;
        }

        for (var i = 0; i < standards.length; i++) {

          if (!standards[i] || !standards[i].standard ) {
            return "";
          }
          var wordArray = standards[i].standard.split(/\W+/);

          var standardLabel = wordArray.length > 6 ? wordArray.splice(0, 6).join(" ") + "..." : wordArray.join(" ");
          out.push(standards[i].dotNotation + ": " + standardLabel);
        }
        return out.join(", ");
      },

      showItemType: function (item) {

        if (item.itemType != "Other") {
          return item.itemType;
        }
        return item.itemType + ": " + item.itemTypeOther;
      },

      showItemTypeAbbreviated: function(item){
        if(!window.fieldValues || !window.fieldValues.itemTypes){
          return "??";
        }

        if (item.itemType != "Other") {
          for(var x in window.fieldValues.itemTypes){
            var keyValue = window.fieldValues.itemTypes[x];
             if( keyValue.value == item.itemType){
               return keyValue.key;
             }
          }
        }
        return "OTH";
      }

    };
  }]);
//ItemFormattingUtils.js