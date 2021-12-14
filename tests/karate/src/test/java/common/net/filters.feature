Feature: Create message filters variables

  Scenario: Setup the filters
    * def messageFilters = Java.type("common.net.MessageFilters")
    * def withMethod = function(msg) { return messageFilters.withMethod(msg) }
