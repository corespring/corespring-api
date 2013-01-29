#!/bin/bash

echo "live"
git remote add corespring-app-live git@heroku.com:corespring-app.git
echo "staging"
git remote add corespring-app-staging git@heroku.com:corespring-app-staging.git
echo "qa"
git remote add corespring-app-qa git@heroku.com:corespring-app-qa.git

echo "remove default heroku"
git remote rm heroku
