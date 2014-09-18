//Output all items contributed by New Classroom,
//that have <tex> tags in their content which contain \frac or ^

//the script does not care about the actual db
//the db, the credentials and the script are set in the parameters to mongo like so
//$ mongo ds063347-a1.mongolab.com:63347/corespring-staging -u corespring -p xxxxxxxx nc-report-usage-of-frac-in-tex-nodes.js

function toId(it) {
  return it._id._id.valueOf() + ":" + it._id.version;
}

function toDescription(it) {
  return it.taskInfo && it.taskInfo.description ? it.taskInfo.description : 'n/a';
}

function filterTex(item) {
  return item && (item.indexOf('\\frac') >= 0 || item.indexOf('^') >= 0)
}

function isolateTex(content) {
  var tex = content.replace('\n', '').split('<tex>');
  tex.shift();
  return tex.
  map(function(item) {
    return item.split('</tex>').shift()
  }).
  filter(filterTex).
  map(function(item) {
    return '<tex>' + item + '</tex>'
  });
}

function toInfo(it) {
  var result = [];
  it.data.files.forEach(function(file) {
    if (file.isMain && file.content) {
      var contents = isolateTex(file.content);
      result = result.concat(contents);
    }
  });
  return result.join('\n');
}

function toUrl(it) {
  return 'http://platform.corespring.org/web#/edit/' + toId(it) + '?panel=metadata';
}

function toOutput(it) {
  return {
    link: toUrl(it),
    description: toDescription(it),
    infoInTexTags: toInfo(it)
  }
}

function printCsv(it) {
  print(it.link + '\t' + it.description + '\t"' + it.infoInTexTags + '"\n')
}

function printCsvHeader(){
  printCsv({link:'link', description:'description', infoInTexTags:'info in tex tags'});
}

function printCsvData(query){
  db.content.find(query).map(toOutput).forEach(printCsv);
}

function main(){
  printCsvHeader();
  printCsvData({
    'contributorDetails.contributor': 'New Classrooms Innovation Partners',
    'data.files.content': {
      $in: [/<tex>.*(\\frac|\^).*<\/tex>/]
    }
  });
}

main();