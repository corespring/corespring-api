function getGrades(dotNotation) {

  function isHs(dotNotation) {
    return (dotNotation.indexOf("HS") == 0);
  }

  function toGradeStrings(arr) {
    function toString(grade) {
      if (!isNaN(grade)) {
        return (grade.toString().length == 1) ? "0" + grade.toString() : grade.toString();
      } else {
        return grade.toString();
      }
    }

    var r = [];
    for (var i in arr) {
      r.push(toString(arr[i]));
    }
    return r;
  }

  function isNumeric(string) {
    return !(string.indexOf("-") >= 0 || isNaN(parseInt(string)));
  }

  function leadingGrade(dotNotation) {
    var leading = dotNotation.split(".")[0];
    if (isNumeric(leading)) {
      return parseInt(leading);
    } else {
      return (leading == "K") ? "K" : undefined;
    }
  }

  function secondaryGrades(dotNotation) {
    function range(string) {
      function intRange(a, b) {
        var r = [];
        for (var i = a; i <= b; i++) {
          r.push(i);
        }
        return r;
      }

      var split = string.split("-");
      if (split.length == 2 && isNumeric(split[0]) && isNumeric(split[1])) {
        return intRange(parseInt(split[0]), parseInt(split[1]));
      } else {
        return undefined;
      }
    }

    var split = dotNotation.split(".");
    var secondary = (split.length > 1) ? split[1] : undefined;
    if (secondary) {
      if (isNumeric(secondary)) {
        return [parseInt(secondary)];
      } else {
        return (secondary == "K") ? "K" : range(secondary);
      }
    } else {
      return undefined;
    }
  }

  if (isHs(dotNotation)) {
    return toGradeStrings([9,10,11,12]);
  } else if (leadingGrade(dotNotation)) {
    return toGradeStrings([leadingGrade(dotNotation)]);
  } else if (secondaryGrades(dotNotation)) {
    return toGradeStrings(secondaryGrades(dotNotation));
  } else {
    return undefined;
  }
}

function up() {
  db.ccstandards.find().forEach(function(standard) {
    if (getGrades(standard.dotNotation)) {
      standard.grades = getGrades(standard.dotNotation);
      db.ccstandards.save(standard);
    }
  });
}

function down() {
  db.ccstandards.find().forEach(function(standard) {
    delete standard.grades;
    db.ccstandards.save(standard);
  });
}

up();