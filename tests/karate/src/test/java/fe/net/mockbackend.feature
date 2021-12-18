@ignore @report=false
Feature: Mock Backend

  Scenario: Setup Mock-backend
    * def newBuffer =
            """
              function() {
                var Queue = Java.type("common.net.MessageQueue")
                return new Queue()
              }
            """
    * def getBackend =
            """
              function() {
                var Backend = Java.type("fe.net.MockBackend")
                return new Backend(newBuffer(), port)
              }
            """

    * def backend = call getBackend
    * karate.log('Backend started at ', backend.getPort())
    * def buffer = backend.getBuffer()

    * def stopBackend = function() { backend.stop() }

    # Shutdown backend automatically after the end of a scenario and  feature
    * configure afterScenario = stopBackend
    * configure afterFeature = stopBackend
