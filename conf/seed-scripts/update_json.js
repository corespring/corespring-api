//Node js script to update the content files
var fs = require('fs');

function adjustDocs(item, save){

  function addIfThere(propertyName, source, target){
    if(source[propertyName]){
      target[propertyName] = source[propertyName];
    }
  }

  function hasProps(o){
    var count = 0;
    for( var i in o){ count++; }
    return count > 0;
  }

  function createOtherAlignments(item){

    var otherAlignments = {};
    addIfThere("bloomsTaxonomy", item, otherAlignments );
    addIfThere("keySkills", item, otherAlignments );
    addIfThere("demonstratedKnowledge", item, otherAlignments );
    addIfThere("relatedCurriculum", item, otherAlignments );

    if(hasProps(otherAlignments)){
      item.otherAlignments = otherAlignments;
    }

    delete item.bloomsTaxonomy;
    delete item.keySkills;
    delete item.demonstratedKnowledge;
    delete item.relatedCurriculum;
  }

  function createTaskInfo(item){

    var taskInfo = {};
    addIfThere("subjects", item, taskInfo);
    addIfThere("gradeLevel", item, taskInfo);
    addIfThere("title", item, taskInfo);
    addIfThere("itemType", item, taskInfo);

    if(hasProps(taskInfo)){
      item.taskInfo = taskInfo;
    }

    delete item.subjects;
    delete item.gradeLevel;
    delete item.title;
    delete item.itemType;
  }

  createOtherAlignments(item);
  createTaskInfo(item);

  save(item);
}


function processFile(name){
  fs.readFile(name, 'utf8', function(err, data){

    if(err){
      console.log(err);
      return;
    }

    var obj = JSON.parse(data);
    console.log(obj);
    adjustDocs(obj, function(updatedObj){
        writeFile( name, JSON.stringify(updatedObj, null, 4));
    });
  });
}

function processLineByLine(name){
  console.log("process line by line: " + name);

  fs.readFile(name, 'utf8', function (err, file) {
      if (err) {
        console.log(err);
        return;
      }

      var arr = file.split("\n");
      console.log( arr.length );

      var processed = [];
      
      function addToProcessed(obj){
        processed.push(JSON.stringify(obj));
      }

      for(var i = 0; i < arr.length; i++){
        var line = arr[i];
        try{
          var obj = JSON.parse(line);
          adjustDocs(obj, addToProcessed);
        } catch (e){
          console.log("couldn't read line: "  + i + ": " + line);
          console.log(e);
        }
      }

      fs.writeFile(name, processed.join("\n"));

  });
}

function writeFile(file, contents){
  fs.writeFile( file, contents, function (err) {
    if (err) return console.log(err);
    console.log('written to ' + file);
  });
}

function processJson(directory){

  var fullPath = process.cwd() + "/" + directory.path;

  if( directory.mode === "line-by-line"){
   processLineByLine(fullPath);
  } else {
    console.log("processing: " + directory );
    fs.readdir(fullPath, function (err, files) {
      if (err) {
        console.log(err);
        return;
      }
      console.log(files);

      for( var i = 0 ; i < files.length ; i++ ){
          processFile(fullPath + "/" + files[i]);
      }
    });
  }
}

var directories = [
{ path: "../seed-data/common/content", mode: "normal"},
{ path: "../seed-data/dev/content", mode : "normal" },
{ path: "../seed-data/exemplar-content/content", mode: "normal"},
{ path: "../seed-data/test/content.json", mode: "line-by-line"}
];

for( var i = 0; i < directories.length; i++){
  var dir = directories[i];
  processJson(dir);
}


