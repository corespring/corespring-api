#!/usr/bin/env ruby
puts "validate_environment.rb - checking environment to see that all the variables are set"

required_env_vars = { 
 :CORESPRING_AMAZON_BUCKET => ENV["CORESPRING_AMAZON_BUCKET"],
 :CORESPRING_AMAZON_ACCESS_KEY => ENV["CORESPRING_AMAZON_ACCESS_KEY"],
 :CORESPRING_AMAZON_SECRET_KEY => ENV["CORESPRING_AMAZON_SECRET_KEY"],
 :CORESPRING_LIVE_DB_URI => ENV["CORESPRING_LIVE_DB_URI"]
} 

throw_error = false

required_env_vars.each{ |k,v|
  if v == nil
    puts "missing env var: #{k}" 
    throw_error = true
  else 
    puts "#{k}: #{v}"
  end
}

raise "!" if throw_error