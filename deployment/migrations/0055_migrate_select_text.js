function up() {

  var removeFeedbackBlock = function(item, id) {
    var regExp = new RegExp('<corespring-feedback-block id="' + id + '"[\\s\\S]*?(\/>|corespring-feedback-block>)', 'gim');
    item.playerDefinition.xhtml = item.playerDefinition.xhtml.replace(regExp, '');
  };

  db.content.find({"playerDefinition": {$exists: true}}).forEach(function(item) {
    var components = item && item.playerDefinition && item.playerDefinition.components ? item.playerDefinition.components : {};
    for (var prop in components) {
      if (components[prop].componentType === 'corespring-select-text') {
        var passage = '';
        var correctResponses = [];
        if (components[prop].model.choices && components[prop].model.choices.length > 0) {
          components[prop].model.choices.forEach(function(choice, index) {
            passage += '<span class="cs-token">' + choice.data + '</span> ';
            if (choice.correct) {
              correctResponses.push(NumberInt(index));
            }
          });
        }
        // Add correctResponse
        components[prop].correctResponse = components[prop].correctResponse ? components[prop].correctResponse : {};
        components[prop].correctResponse.value = components[prop].correctResponse.value ? components[prop].correctResponse.value : correctResponses;
        // Make sure that maxSelections is valid according to the amount of correctResponses
        if (components[prop].model.config.maxSelections) {
          if (components[prop].model.config.maxSelections > 0 && correctResponses.length > components[prop].model.config.maxSelections) {
            components[prop].model.config.maxSelections = NumberInt(correctResponses.length);
          } else {
            components[prop].model.config.maxSelections = NumberInt(components[prop].model.config.maxSelections);
          }
        } else {
          components[prop].model.config.maxSelections = NumberInt(0);
        }
        // Add passage field
        components[prop].model.config.passage = components[prop].model.config.passage ? components[prop].model.config.passage : passage;
        // Add new availability field
        components[prop].model.config.availability = components[prop].model.config.availability ? components[prop].model.config.availability : 'all';
        // Add new label field
        components[prop].model.config.label = components[prop].model.config.label ? components[prop].model.config.label : '';
        // Add title
        components[prop].title = 'Select Evidence in Text';
        // Add weight
        components[prop].weight = NumberInt(1);
        // Add partial scoring property
        components[prop].allowPartialScoring = false;
        components[prop].partialScoring = [{}];
        // Add feedback
        components[prop].feedback = {
          correctFeedbackType: "none",
          partialFeedbackType: "none",
          incorrectFeedbackType: "none"
        };
        // Remove unnecessary fields
        components[prop].model.choices = [];
        delete components[prop].model.config.showFeedback;
        delete components[prop].model.config.checkIfCorrect;
        delete components[prop].model.config.minSelections;
        // This inner loop iterates over feedback block components related to the select text component
        for (var comp in components) {
          var normalizedPropName = comp.toLowerCase();
          if (normalizedPropName.indexOf(prop.toLowerCase() + '_feedback') >= 0) {
            if (normalizedPropName.indexOf('responsescorrect') >= 0
                && components[prop].feedback.correctFeedbackType) {
              if (components[comp].feedback
                  && components[comp].feedback.outcome
                  && components[comp].feedback.outcome.responsesCorrect
                  && components[comp].feedback.outcome.responsesCorrect.text) {
                components[prop].feedback.correctFeedbackType = 'custom';
                components[prop].feedback.correctFeedback = components[comp].feedback.outcome.responsesCorrect.text.replace(/(<([^>]+)>)/ig, '').trim();
                delete components[comp];
                removeFeedbackBlock(item, comp);
              }
            }
            if (normalizedPropName.indexOf('responsesincorrect') >= 0) {
              if (components[comp].feedback
                  && components[comp].feedback.outcome
                  && components[comp].feedback.outcome.responsesIncorrect
                  && components[comp].feedback.outcome.responsesIncorrect.text) {
                components[prop].feedback.incorrectFeedbackType = 'custom';
                components[prop].feedback.incorrectFeedback = components[comp].feedback.outcome.responsesIncorrect.text.replace(/(<([^>]+)>)/ig, '').trim();
                delete components[comp];
                removeFeedbackBlock(item, comp);
              }
            }
            if (normalizedPropName.indexOf('exceedmax') >= 0
                || normalizedPropName.indexOf('belowmin') >= 0
                || normalizedPropName.indexOf('numbercorrect') >= 0) {
              delete components[comp];
              removeFeedbackBlock(item, comp);
            }
          }
        }
        // Save item
        db.content.save(item);
      }
    }
  });
}

function down() {
  print("Irreversible migration.");
}
