var startTag = "<tex>";
var endTag = "</tex>";

var latexHelper = {

  /**
   * Returns true if the content contains LaTeX tags.
   */
  hasLatex: function(content) {
    return content.indexOf(startTag) >= 0;
  },

  /**
   * Returns all LaTeX tags present in the content in an array.
   */
  getLatexTags: function(content) {
    var latex = [];
    for(var i = 0; i <= content.length - startTag.length; i++) {
      if (content.toLowerCase().substr(i, startTag.length) === startTag) {
        for(var j = i; j <= content.length - endTag.length; j++) {
          if (content.toLowerCase().substr(j, endTag.length) === endTag) {
            latex.push(content.substring(i, j + endTag.length));
            break;
          }
        }
      }
    }
    return latex;
  },

  /**
   * Returns the content of all LaTeX tags present in the content in an array.
   */
  getLatexContent: function(content) {
    var tags = this.getLatexTags(content);
    var content = [];
    for (var i in tags) {
      content.push(tags[i].substring(startTag.length, tags[i].length - endTag.length));
    }
    return content;
  },

  /**
   * Removes display modifiers from a string of LaTeX content.
   */
  removeDisplayModifiers: function(latex) {
    var displayModifiers = ['small', 'displaystyle', 'normalsize', 'large'];
    var result = latex;
    displayModifiers.forEach(function(modifier) {
      result = result.replace(RegExp('[\\\\]' + modifier, 'g'), '');
    });
    return result;
  },

  /**
   * Removes empty text nodes from a string of LaTeX content.
   */
  removeEmptyText: function(latex) {
    var emptyTextStrings = ['text{}', 'text {}', 'text { }', 'text{ }'];
    var result = latex;
    emptyTextStrings.forEach(function(emptyTextString) {
      result = result.replace(RegExp('[\\\\]' + emptyTextString, 'g'), '');
    });
    return result;
  },

  /**
   * Adds a '\displaystyle' prefix to LaTeX content if it includes either '\frac' or '^'.
   */
  addDisplayStyle: function(latex) {
    function needsDisplayStyle(latexItem) {
      var modifiersRequiringDisplayStyle = ['\\frac', '^'];
      var result = false;
      modifiersRequiringDisplayStyle.forEach(function(modifier) {
        if (latexItem.indexOf(modifier) >= 0) {
          result = true;
        }
      });
      return result;
    }
    if (needsDisplayStyle(latex)) {
      return '\\displaystyle ' + latex.trim();
    } else {
      return latex.trim();
    }
  },

  /**
   * Processes a string of LaTeX content to remove display modifiers, remove empty text, and add a `\displaystyle`
   * modifier if necessary.
   */
  processLatex: function(latex) {
    return this.addDisplayStyle(
      this.removeEmptyText(
        this.removeDisplayModifiers(latex)
      )
    );
  }
};

function up() {
  var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";
  var newClassroomsContent = db.content.find({ "collectionId": NEW_CLASSROOMS_COLLECTION_ID });
  
  newClassroomsContent.forEach(function(content) {
    content.data.files.forEach(function(file) {
      if (file.isMain && file.content && latexHelper.hasLatex(file.content)) {
        file.content.replace(/<tex>(.*?)<\/tex>/g, function(latex) {
          return latexHelper.processLatex(latex);
        });
        db.content.save(content);
      }
    });
  });
}

function down() {
  // Irreversible migration.
}