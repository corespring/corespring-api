#!/usr/bin/env ruby
require 'json'

begin
  app = ARGV[1].chomp
  puts "0: #{ARGV[0]}"
  puts "app name: #{app}"
  `heroku config:set ENV_INIT_DATA="true" --app #{app}` unless app.nil?
rescue
  puts "error setting hash as env var"
end