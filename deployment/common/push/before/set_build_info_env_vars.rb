#!/usr/bin/env ruby
require 'json'
require 'rest-client'

begin

  def api_key
    net_rc = open(File.expand_path("~/.netrc")).read
    regex = /machine code.heroku.com.*password\s(.*?)$/m

    if( net_rc.match(regex))
      match, key = *net_rc.match(regex)
      key
    else
      raise "can't find key"
    end
  end


  puts "!-------- set build info env vars -------------"
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

  vars = {
    :ENV_CORESPRING_API_COMMIT_HASH => commit_hash,
    :ENV_CORESPRING_API_COMMIT_HASH_SHORT => commit_hash_short,
    :ENV_CORESPRING_API_COMMIT_MSG => commit_msg,
    :ENV_CORESPRING_API_PUSH_DATE => push_date,
    :ENV_CORESPRING_API_BRANCH => branch
  }

  unless app.nil?
    heroku = RestClient::Resource.new( "https://api.heroku.com/apps",:user => '', :password => api_key)
    heroku["#{app}/config-vars"].patch(vars.to_json, :content_type => :json, :accept => "application/vnd.heroku+json; version=3" )
  end

rescue
  puts "error setting hash as env var"
end