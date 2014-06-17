regressionTestRunner = require('regression-test-runner')

module.exports = (grunt) ->

  config =
    regressionTestRunner:
      tests: ['tests/**/*.js']

  grunt.initConfig(config)

  grunt.registerTask('regression', regressionTestRunner(grunt).help, regressionTestRunner(grunt).task)
  grunt.registerTask('default', 'regression')



