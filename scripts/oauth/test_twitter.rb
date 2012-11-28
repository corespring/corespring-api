require 'oauth'

puts "A little script to test oauth with twitter."

consumer_key = ARGV[0]
consumer_secret = ARGV[1]
user_key = ARGV[2]
user_secret = ARGV[3]

def prepare_access_token(oauth_token, oauth_token_secret)
  consumer = OAuth::Consumer.new(consumer_key, consumer_secret,
                                 { :site => "https://api.twitter.com",
                                   :scheme => :header
  })
  # now create the access token object from passed values
  token_hash = { :oauth_token => oauth_token,
    :oauth_token_secret => oauth_token_secret
  }
  access_token = OAuth::AccessToken.from_hash(consumer, token_hash )
  return access_token
end
#                                        
#                                        # Exchange our oauth_token and oauth_token secret for the AccessToken instance.
access_token = prepare_access_token(user_key, user_secret)
#                                        # use the access token as an agent to get the home timeline
response = access_token.request(:get, "https://api.twitter.com/1.1/account/verify_credentials.json")

puts response
puts response.body
