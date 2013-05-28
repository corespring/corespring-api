angular.module('corespring-utils', []);

angular.module('corespring-utils')
  .factory('ResourceUtils', [function () {

    var urls = {
      renderResource:'/web/show-resource/{key}/main',
      printResource:'/web/print-resource/{key}/main'
    };

    /**
     * Note: In our resources we sometimes deliver assets that are referred to within the resources
     * using a relative path. For this reason all resources need to be identified on the same
     * virtual path level as the resource, so that they can be found. So the 'main' here is important.
     * @param key
     * @param forPrinting
     * @returns {string}
     */
    var getResourceUrl = function (key, forPrinting) {
      var template= forPrinting ? urls.printResource : urls.renderResource;
      return template.replace("{key}", key);
    };

    return {

      getItemSrc: function (itemData, forPrinting) {
        if (itemData == undefined) return null;
        return getResourceUrl(itemData.id, forPrinting);
      },

      getSmSrc: function (itemData, sm, forPrinting) {
        if (itemData == undefined) return null;
        return getResourceUrl(itemData.id + "/" + sm.name, forPrinting);
      },

      getLicenseTypeUrl: function (licenseType) {
        return licenseType ? "/assets/images/licenseTypes/" + licenseType + ".png" : undefined;
      }
    }
  }]);

angular.module('corespring-utils')
  .factory('ItemFormattingUtils', [function () {

    return {


      getCopyrightUrl: function (item) {
        if (!item) return;

        var key = item.copyrightOwner;

        if (!key) return;

        var map = {
          "New York State Education Department": "nysed.png",
          "State of New Jersey Department of Education": "njded.png",
          "Illustrative Mathematics": "illustrativemathematics.png",
          "Aspire Public Schools": "aspire.png",
          "College Board": "CollegeBoard.png",
          "New England Common Assessment Program": "NECAP.jpg",
          "LearnZillion": "lzlogo-png.png",
          "Expeditionary Learning": "El.png",
          "Smarter Balanced Assessment Consortium": "smarter.png",
          "TIMSS": "TIMSS.jpeg"
        };

        if (!map[key]) return;
        return  "/assets/images/copyright/" + map[key];
      },

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

      getShortSubjectLabel: function (subject) {
        var out = this.getPrimarySubjectLabel(subject);
        return out
          .replace("Mathematics", "Math")
          .replace("English Language Arts", "ELA");

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

          if (!standards[i] || !standards[i].standard) {
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

      showItemTypeAbbreviated: function (item) {
        if (!window.fieldValues || !window.fieldValues.itemTypes) {
          return "??";
        }

        if (item.itemType != "Other") {
          for (var x in window.fieldValues.itemTypes) {
            var keyValue = window.fieldValues.itemTypes[x];
            if (keyValue.value == item.itemType) {
              return keyValue.key;
            }
          }
        }
        return "OTH";
      },

      prependHttp: function (url) {
        if (!url) return "";
        if (!url.match(/^[a-zA-Z]+:\/\//)) {
          url = 'http://' + url;
        }
        return url;
      },

      getAuthorAbbreviation: function (author) {
        if (!author || author.length == 0) return "";

        var contributorNameToAbbreviationMap = {
          "State of New Jersey Department of Education": "NJDOE",
          "New York State Education Department": "NYSED",
          "Illustrative Mathematics": "Illustrative"
        };

        if (contributorNameToAbbreviationMap[author]) {
          return contributorNameToAbbreviationMap[author];
        }

        var abbreviate = function (s) {
          var words = s.split(" ");
          var firstLetters = _.map(words, function (w) {
            return w.substring(0, 1)
          });
          return firstLetters.join("").toUpperCase();
        };

        return author.indexOf(" ") != -1 ? abbreviate(author) : author;
      }

    };
  }]);
