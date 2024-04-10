@ignore @report=false
Feature: current env page object
  Scenario:
    * def page_object = 'classpath:fe/utils/<env>.feature@name=<name>'
    * replace page_object.env = karate.env
    * replace page_object.name = name
    * call read(page_object)
