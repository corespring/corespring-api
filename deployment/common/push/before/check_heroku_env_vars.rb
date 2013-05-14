#!/usr/bin/env ruby

puts "check that all the required heroku env vars have been set"

required_env_vars = [
 "TWITTER_CONSUMER_KEY",
 "TWITTER_CONSUMER_SECRET",
 "GOOGLE_CLIENT_ID",
 "GOOGLE_CLIENT_SECRET",
 "ENV_MONGO_URI",
 "ENV_INIT_DATA",
 "ENV_DEMO_ORG_ID"
]

throw_error = false

missing = []
required_env_vars.each{ |key|
  cmd = "heroku config:get #{key}"
  result = `cmd`
  missing << key if result.chomp.empty?
}

raise "missing env vars: #{missing}" if missing.length > 0