###
# We are patch fixing the app.
# The latest fix points to cloud mongodb
# We point to jdk 1.8 here - but at runtime it's not used (cos cbt isn't packing it correctly)
# The app falls back to the heroku jdk 1.7.0_201 - This version of 1.7 has the cert fix we need
# to make a mongo db connection see: https://docs.atlas.mongodb.com/reference/faq/security/#java-users
#
# We could look at fixing cbt to package 1.8 correctly so that it is picked up,
# but because this is just a patch fix and this app will be decommissioned soon it's
# probably not with the effort.
# Instead we'll live with this weird setup where we add 1.8 to the slug, knowing that it won't work
#
###


play universal:packageZipTarball


cbt slug-mk-from-artifact-file \
--artifact-file target/universal/corespring-root-$VERSION.tgz \
--out-path target/slug-$VERSION.tgz --platform jdk-1.8

cbt slug-deploy-from-file \
--heroku-app=$APP \
--slug-file target/slug-$VERSION.tgz