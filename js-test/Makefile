
APP_JS_SRC_FILES = $(shell find ../public/js/corespring -type f -path '**/*.js' )
SPEC_FILES = $(shell find ./unit -type f -path '**/*.js')

JS_TEST_FILES = $(shell find modules -type f -path '*/test/*.js')
COFFEE_FILES = $(shell find . -type f -name '*.coffee')
APP_VENDOR = "all_vendor"
APP_CORESPRING = "all_corespring"

js:
	cat ${APP_JS_SRC_FILES} > all_corespring.js
	cat ${SPEC_FILES} > all_specs.js

build: js
