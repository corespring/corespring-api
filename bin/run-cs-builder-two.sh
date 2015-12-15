#!/usr/bin/env bash

cbt artifact-mk-from-git \
--git="git@github.com:corespring/corespring-api.git" \
--cmd="play universal:packageZipTarball" \
--branch="feature/release-packaging" \
--artifact="target/universal/corespring-root-(.*).tgz"

cbt artifact-list \
--git="git@github.com:corespring/corespring-api.git" \
--branch="feature/release-packaging"

cbt artifact-deploy-from-branch \
--git="git@github.com:corespring/corespring-api.git" \
--heroku-app="new-slug-deploy-test" \
--platform="jdk-1.7" \
--branch="feature/release-packaging"
