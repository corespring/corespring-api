
require 'aws/s3'
require 'pp'

access_key = "AKIAJNPUNTVH2HMFWVVA"
secret_key = "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"
bucket = "corespring-assets-test"


AWS::S3::Base.establish_connection!(
      :access_key_id     => access_key,
      :secret_access_key => secret_key
    )

s3_bucket = AWS::S3::Bucket.find(bucket)



def upload(name) = IsAuthenticated(request)(BodyParser)
