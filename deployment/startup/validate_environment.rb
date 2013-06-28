#!/usr/bin/env ruby
puts "validate_environment.rb - checking environment to see that all the variables are set"

keys = [
 "CORESPRING_AMAZON_BUCKET",
 "CORESPRING_AMAZON_ACCESS_KEY",
 "CORESPRING_AMAZON_SECRET_KEY",
 "CORESPRING_LIVE_DB_URI",
 "CORESPRING_LIVE_DB_REPLICA_SET_NAME"]

key_values = Hash.new

keys.each{ |k| key_values[k] = ENV[k]}

throw_error = false

key_values.each{ |k,v|
  if v == nil
    puts "missing env var: #{k}"
    throw_error = true
  end
}

raise "!" if throw_error