# TODO: flesh out....

play universal:packageZipTarball


cbt slug-mk-from-artifact-file \
--artifact-file target/universal/corespring-root-$VERSION.tgz \
--out-path target/slug-$VERSION.tgz --platform jdk-1.7

cbt slug-deploy-from-file \
--heroku-app=$APP \
--slug-file target/slug-7.9.12.tgz