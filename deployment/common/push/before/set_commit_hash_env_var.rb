#!/usr/bin/env ruby
require 'json'

begin
  commit_hash = `git rev-parse --short HEAD`
  app = ARGV[1]
  `heroku config:set ENV_CORESPRING_API_COMMIT_HASH=#{commit_hash} --app #{app}` unless app.nil?
rescue
  puts "error setting hash as env var"
end