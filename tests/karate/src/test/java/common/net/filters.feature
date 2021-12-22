Feature: Create message filters variables

  Scenario: Setup the message filters
    * def messageFilters = Java.type("common.net.MessageFilters")
    # Filter that accept only message where the method match
    * def withMethod = function(msg) { return messageFilters.withMethod(msg) }
