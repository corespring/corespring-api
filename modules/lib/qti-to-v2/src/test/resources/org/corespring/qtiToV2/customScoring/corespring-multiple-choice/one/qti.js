var correctAnswers = 0;
if (RESPONSE.value.indexOf("1") != -1) correctAnswers += 1;
if (RESPONSE.value.indexOf("2") != -1) correctAnswers += 1;
if (RESPONSE.value.indexOf("3") != -1) correctAnswers += 1;

var score = 0;
if (correctAnswers == 1) score = 0.5
if (correctAnswers == 2) score = 0.8
if (correctAnswers == 3) score = 1.0

var outcome = {};
outcome.score = score;
outcome;
