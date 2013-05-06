#!/usr/bin/env ruby
require 'json'

begin
  commit_hash_short = `git rev-parse --short HEAD`.chomp
  commit_hash = `git rev-parse HEAD`.chomp
  commit_msg = `git rev-list --format=%s%B --oneline --max-count=1 HEAD`
  push_date = `date`
  app = ARGV[1].chomp
  puts "0: #{ARGV[0]}"
  puts "app name: #{app}"
  `heroku config:set ENV_CORESPRING_API_COMMIT_HASH="#{commit_hash}" --app #{app}` unless app.nil?
  `heroku config:set ENV_CORESPRING_API_COMMIT_HASH_SHORT="#{commit_hash_short}" --app #{app}` unless app.nil?
  `heroku config:set ENV_CORESPRING_API_COMMIT_MSG="#{commit_msg}" --app #{app}` unless app.nil?
  `heroku config:set ENV_CORESPRING_API_PUSH_DATE="#{push_date}" --app #{app}` unless app.nil?

rescue
  puts "error setting hash as env var"
end