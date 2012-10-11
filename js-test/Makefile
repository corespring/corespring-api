
DECLARATIONS = $(shell find ../public/js/corespring -type f -path '**/services.js' )
APP_JS_SRC_FILES = $(shell find ../public/js/corespring -type f -path '**/*.js' )
SPEC_FILES = $(shell find ./unit -type f -path '**/*.js')

js:
	cat ${DECLARATIONS}  ${APP_JS_SRC_FILES} > all_corespring.js
	cat ${SPEC_FILES} > all_specs.js

build: js
