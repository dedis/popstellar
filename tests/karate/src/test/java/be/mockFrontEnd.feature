@ignore @report=false
Feature: Mock FrontEnd

  Scenario: creates a valid FrontEnd
    * def frontends = []
    * def createFrontend =
      """
        function(){
          var Frontend = Java.type('be.utils.Frontend')
          var frontend = new Frontend(wsURL)
          frontends.push(frontend)
          return frontend
        }
      """

    * def stopAllFrontends =
      """
        function() {
          for (var i = 0; i < frontends.length; i++) {
            frontends[i].close();
          }
          stopServer();
          deleteDB();
        }
      """

    # Shutdown frontend automatically after the end of a scenario and  feature
    * configure afterScenario = stopAllFrontends
    * configure afterFeature = stopAllFrontends
