@ignore @report=false
Feature: Mock Backend

  Scenario: Setup Mock-backend
    * def getBackend =
            """
              function() {
                var Backend = Java.type("fe.utils.MockBackend")
                return new Backend(9005)
              }
            """

    * def backend = call getBackend
    * karate.log('Backend started at ', backend.getPort())

    * def stopBackend = function() { backend.stop() }

    # Shutdown backend automatically after the end of a scenario and  feature
    * configure afterScenario = stopBackend
    * configure afterFeature = stopBackend
