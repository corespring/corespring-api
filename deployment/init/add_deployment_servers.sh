#!/bin/bash

echo "live"
git remote add corespring-app-live git@heroku.com:corespring-app.git

echo "staging"
git remote add corespring-app-staging git@heroku.com:corespring-app-staging.git

echo "qa"
git remote add corespring-app-qa git@heroku.com:corespring-app-qa.git

echo "content-team"
git remote add corespring-app-content-team git@heroku.com:cs-content-team-integration.git

echo "corespring-app-branch-one"
git remote add corespring-app-branch-one git@heroku.com:corespring-app-branch-one.git

echo "corespring-app-branch-two"
git remote add corespring-app-branch-two git@heroku.com:corespring-app-branch-two.git

echo "remove default heroku"
git remote rm heroku
