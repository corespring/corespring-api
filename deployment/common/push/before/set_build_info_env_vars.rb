#!/usr/bin/env ruby
require 'json'

begin
  puts "-------- set build info env vars -------------"
  ### Args: 0: tmp json file with heroku config, 1: app name, 2: branch name
  app = ARGV[1].chomp
  puts "config json: #{ARGV[0]}"
  puts "app name: #{app}"
  branch = ARGV[2]
  puts "branch : #{branch}"
  commit_hash_short = `git rev-parse --short origin/#{branch}`.chomp
  commit_hash = `git rev-parse origin/#{branch}`.chomp
  commit_msg = `git rev-list --format=%s%B --oneline --max-count=1 origin/#{branch}`
  push_date = `date`
  `heroku config:set ENV_CORESPRING_API_COMMIT_HASH="#{commit_hash}" --app #{app}` unless app.nil?
  `heroku config:set ENV_CORESPRING_API_COMMIT_HASH_SHORT="#{commit_hash_short}" --app #{app}` unless app.nil?
  `heroku config:set ENV_CORESPRING_API_COMMIT_MSG="#{commit_msg}" --app #{app}` unless app.nil?
  `heroku config:set ENV_CORESPRING_API_PUSH_DATE="#{push_date}" --app #{app}` unless app.nil?

rescue
  puts "error setting hash as env var"
end