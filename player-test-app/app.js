var fs = require('fs')
  , express = require('express');

var app = express.createServer();

app.configure(function() {
  app.use(express.static(__dirname + '/public'));
});

app.listen(5000, function() {
  var address = app.address().address;
  var port = app.address().port;
  console.log("server started on http://" + address + ':' + port);
});

