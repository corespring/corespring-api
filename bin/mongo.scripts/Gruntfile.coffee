fs = require "fs"
_ = require "lodash"


module.exports = (grunt) ->

  commonConfig =
    app: "."

  config =
    pkg: grunt.file.readJSON('package.json')
    common: commonConfig

    jasmine:
      unit:
        src: 'src/main/*.js'
        options:
          keepRunner: true
          vendor: [
          ]
          specs: 'src/test/*-spec.js'

    jshint:
      options:
        jshintrc: '.jshintrc'
      main: ['src/**/*.js']

    watch:
      js:
        files: ['src/**/*.js']
        tasks: ['jshint:main']

    jsbeautifier:
      files : ["src/**/*.js"],
      options :
        js:
          braceStyle: "collapse"
          breakChainedMethods: false
          e4x: false
          evalCode: false
          indentChar: " "
          indentLevel: 0
          indentSize: 2
          indentWithTabs: false
          jslintHappy: false
          keepArrayIndentation: true
          keepFunctionIndentation: true
          maxPreserveNewlines: 10
          preserveNewlines: true
          spaceBeforeConditional: true
          spaceInParen: false
          unescapeStrings: false
          wrapLineLength: 0


  grunt.initConfig(config)

  npmTasks = [
    'grunt-contrib-jasmine'
    'grunt-contrib-clean'
    'grunt-contrib-watch'
    'grunt-contrib-jshint'
    'grunt-jsbeautifier'
  ]



  grunt.loadNpmTasks(t) for t in npmTasks
  grunt.registerTask('test', 'test client side js', ['jasmine:unit'])
  grunt.registerTask('default', ['jshint', 'test'])
