function up() {
  function DomainHelper() {
    var domains = (function() {
      var domains = {};
      var domainMapper = {
        'ELA-Literacy' : 'subCategory',
        'ELA' : 'subCategory',
        'Math' : 'category'
      };

      db.ccstandards.find().forEach(function(standard) {
        domains[standard.dotNotation] = standard[domainMapper[standard.subject]];
      });
      return domains;
    })();

    this.domainsFor = function(item) {
      var returnValue = [];
      if (item.standards) {
        for (var i in item.standards) {
          var domain = domains[item.standards[i]];
          if (returnValue.indexOf(domain) < 0) {
            returnValue.push(domain);
          }
        }
      }
      return returnValue;
    }
  }

  var domainHelper = new DomainHelper();

  db.content.find({'contentType': 'item'}).forEach(function(item) {
    item.taskInfo = item.taskInfo || {};
    item.taskInfo.domains = domainHelper.domainsFor(item);
    db.content.save(item);
  });

}

function down() {
  db.content.update({}, {$unset: {'taskInfo.domains' : ''}}, {'multi': true});
}

up();