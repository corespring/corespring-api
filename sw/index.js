var express = require("express");
var app = express();
var { readFileSync } = require("fs-extra");
const { resolve } = require("path");

// respond with "hello world" when a GET request is made to the homepage
app.get("/", function(req, res) {
  const html = readFileSync(resolve(__dirname, "index.html"), "utf8");
  res.send(html);
});

app.get("/cs-api-sw.js", (req, res) => {
  const js = readFileSync(
    resolve(__dirname, "../app/web/controllers/views/ServiceWorker.scala.js"),
    "utf8"
  );
  res.setHeader("Content-Type", "text/javascript");
  res.send(js);
});

app.listen(9001, () => {
  console.log("listening");
});
