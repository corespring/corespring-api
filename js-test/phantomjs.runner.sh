#!/bin/bash

#aggregate js and spec files
DECLARATIONS=`find ../public/js/corespring -type f \( -name services.js -o -name Logger.js \)`

##We are ignoring the print related directives here
#be sure to add app files first before others so main variable declarations come first
APP_JS_SRC_FILES=`find ../public/js/corespring -type f -path '**/app.js'| grep -v '/printing/' | grep -v '/aggregate/' | grep -v '/instructor/'`
OTHER_APP_JS_SRC_FILES=`find ../public/js/corespring -type f -path '**/*.js' ! -iname 'app.js'| grep -v '/printing/' | grep -v '/aggregate/' | grep -v '/instructor/' | grep -v 'corespring-error-player.js' | grep -v 'Logger.js'`
TEST_LIB_FILES="./lib/jasmine-jquery.js"
FRONTLOAD_SPEC_FILES=`find ./unit -type f -path '**/*-priority-1.js'`
SPEC_FILES=`find ./unit \( -type f -path '**/*.js' -and -not -name '*priority*' \)`

cat ${DECLARATIONS} ${APP_JS_SRC_FILES} ../public/js/corespring/qti/services/qtiServices.js ${OTHER_APP_JS_SRC_FILES} ${TEST_LIB_FILES} > all_corespring.js
cat ${FRONTLOAD_SPEC_FILES} ${SPEC_FILES} > all_specs.js

# sanity check to make sure phantomjs exists in the PATH
hash /usr/bin/env phantomjs &> /dev/null
if [ $? -eq 1 ]; then
    echo "ERROR: phantomjs is not installed"
    echo "Please visit http://www.phantomjs.org/"
    exit 1
fi

# sanity check number of args
if [ $# -lt 1 ]
then
    echo "Usage: `basename $0` path_to_runner.html"
    echo
    exit 1
fi

SCRIPTDIR=$(dirname `perl -e 'use Cwd "abs_path";print abs_path(shift)' $0`)
TESTFILE=""
while (( "$#" )); do
    TESTFILE="$TESTFILE `perl -e 'use Cwd "abs_path";print abs_path(shift)' $1`"
    shift
done

# cleanup previous test runs
cd $SCRIPTDIR
rm -f *.xml

# make sure phantomjs submodule is initialized
cd ..
git submodule update --init

# fire up the phantomjs environment and run the test
cd $SCRIPTDIR
/usr/bin/env phantomjs $SCRIPTDIR/phantomjs-testrunner.js $TESTFILE

exit $?
